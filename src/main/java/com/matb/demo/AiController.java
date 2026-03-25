package com.matb.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private static final Pattern TICKER_PATTERN = Pattern.compile("\\b([A-Z]{1,5})\\b");
    private static final Set<String> COMMON_WORDS = Set.of(
            "A", "THE", "AND", "OR", "IS", "AS", "TO", "FOR", "FROM", "IN", "ON", "AT", "BY", "BE", "IT", "ARE"
    );

    private static final String SYSTEM_PROMPT = """
            You are an expert financial assistant specializing in market and equity analysis.
            Always use the get_advanced_stats tool to retrieve real data before responding, also you can count with,
            get_news_headlines brings news from the provided tickers.

            CRITICAL: Your response MUST be a single valid JSON object.
            No markdown fences, no explanation text before or after. Follow this schema exactly:

            {
              "overview":  "<2-3 sentence company and market context summary>",
              "metrics": {
                "price":            <number or null>,
                "rsi":              <number or null>,
                "bollingerUpper":   <number or null>,
                "bollingerLower":   <number or null>,
                "movingAverage20d": <number or null>,
                "annualVolatility": <number or null>,
                "extras":           {}
              },
              "analysis":  "<detailed insight — use \\n\\n to separate paragraphs>",
              "risks":     ["<risk 1>", "<risk 2>", "<risk 3>"],
              "summary":   "<1-2 sentence conclusion>"
            }

            Rules:
            - All fields are required. Use null for unavailable numeric metrics.
            - risks must be a JSON array of strings, one item per risk.
            - analysis paragraphs separated with \\n\\n inside the JSON string value.
            - Do NOT wrap the response in ```json``` or any other text.
            """;

    private final ChatClient   chatClient;
    // Spring Boot auto-configures an ObjectMapper bean via JacksonAutoConfiguration.
    // We use @Qualifier to be explicit and avoid ambiguity if other mappers exist.
    private final ObjectMapper objectMapper;

    public AiController(ChatClient chatClient,
                        ToolCallbackProvider toolCallbackProvider,
                        ObjectMapper objectMapper) {
        this.chatClient   = chatClient;
        this.objectMapper = objectMapper;
        log.info("AiController initialized");
    }

    // =========================================================================
    //  POST /api/ai/chat  →  Mono<ChatMessage>
    // =========================================================================

    @PostMapping("/chat")
    public Mono<ChatMessage> chat(@RequestBody ChatQuery query) {
        log.debug("POST /chat - prompt: {}", query.prompt());

        if (query.prompt() == null || query.prompt().isBlank()) {
            return Mono.just(errorResponse(null, "Empty prompt", null));
        }

        final String userPrompt = query.prompt().trim();
        final String ticker     = extractTicker(userPrompt);
        final long   start      = System.currentTimeMillis();

        return Mono.fromCallable(() -> {

                    // 1. Call model -------------------------------------------------------
                    ChatResponse chatResponse = chatClient.prompt()
                            .system(SYSTEM_PROMPT)
                            .user(userPrompt)
                            .call()
                            .chatResponse();

                    String raw = chatResponse.getResult().getOutput().getText();
                    log.debug("Raw model output ticker={}: {}", ticker, raw);

                    // 2. Token usage + model name -----------------------------------------
                    Usage  usage        = chatResponse.getMetadata().getUsage();
                    String model        = chatResponse.getMetadata().getModel();
                    int    inputTokens  = usage != null ? (int) usage.getPromptTokens()     : 0;
                    int    outputTokens = usage != null ? (int) usage.getCompletionTokens() : 0;

                    // 3. Parse JSON → EquityAnalysis --------------------------------------
                    ChatMessage.EquityAnalysis sections;
                    String         status;

                    try {
                        String json = raw.trim()
                                .replaceAll("(?s)^```json\\s*", "")
                                .replaceAll("(?s)^```\\s*",     "")
                                .replaceAll("```$",              "")
                                .trim();

                        sections = objectMapper.readValue(json, ChatMessage.EquityAnalysis.class);
                        status   = "SUCCESS";

                    } catch (Exception parseEx) {
                        log.warn("JSON parse failed — PARTIAL fallback. Cause: {}", parseEx.getMessage());
                        sections = new ChatMessage.EquityAnalysis(raw, null, null, null, null);
                        status   = "PARTIAL";
                    }

                    // 4. Assemble response ------------------------------------------------
                    ChatMessage.ResponseMetadata metadata = new ChatMessage.ResponseMetadata(
                            status, model,
                            inputTokens, outputTokens,
                            System.currentTimeMillis() - start,
                            null
                    );

                    return new ChatMessage(ticker, Instant.now(), sections, metadata);

                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> {
                    log.error("Chat error: {}", ex.getMessage(), ex);
                    return Mono.just(errorResponse(
                            ticker, ex.getMessage(), System.currentTimeMillis() - start));
                });
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private ChatMessage errorResponse(String ticker, String msg, Long elapsedMs) {
        return new ChatMessage(
                ticker,
                Instant.now(),
                null,
                new ChatMessage.ResponseMetadata("ERROR", null, null, null, elapsedMs, msg)
        );
    }

    private String extractTicker(String prompt) {
        if (prompt == null || prompt.isBlank()) return null;
        var matcher = TICKER_PATTERN.matcher(prompt.toUpperCase());
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (!COMMON_WORDS.contains(candidate)) return candidate;
        }
        return null;
    }

    // =========================================================================
    //  Request DTO
    // =========================================================================

    public record ChatQuery(String prompt, String context, String mode) {
        public ChatQuery(String prompt) { this(prompt, null, "default"); }
    }
}