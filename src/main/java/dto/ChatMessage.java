package dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

// =============================================================================
//  Top-level response — Mono<ChatMessage> payload
// =============================================================================

/**
 * Root response object returned by POST /api/ai/chat.
 *
 * <pre>
 * {
 *   "ticker":    "AAPL",
 *   "timestamp": "2024-12-01T18:30:00Z",
 *   "sections": { ... },       // typed content by section
 *   "metadata": { ... },       // model/token/status info
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        String              ticker,
        Instant             timestamp,
        EquityAnalysis      sections,
        ResponseMetadata    metadata
) {

    // -------------------------------------------------------------------------
    //  sections — typed content structure
    // -------------------------------------------------------------------------

    /**
     * All content sections of the equity analysis.
     * Every field is nullable — not all queries will produce all sections.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EquityAnalysis(
            String          overview,           // 2-3 sentence company summary
            KeyMetrics      metrics,            // structured price/technical data
            String          analysis,           // narrative insight paragraphs
            List<String>    risks,              // bullet-level risk items
            String          summary             // 1-2 sentence conclusion
    ) {}

    /**
     * Key financial and technical metrics extracted from get_advanced_stats.
     * All numeric fields are Double to allow null when data is unavailable.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record KeyMetrics(
            Double          price,
            Double          rsi,
            Double          bollingerUpper,
            Double          bollingerLower,
            Double          movingAverage20d,
            Double          annualVolatility,
            // Extra passthrough bucket for any metric not modeled above
            Map<String, Object> extras
    ) {}

    // -------------------------------------------------------------------------
    //  metadata — request / response envelope info
    // -------------------------------------------------------------------------

    /**
     * Metadata about the model invocation.
     * status values: SUCCESS | PARTIAL | ERROR
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ResponseMetadata(
            String  status,             // SUCCESS | PARTIAL | ERROR
            String  model,              // e.g. "gpt-4o", "claude-3-5-sonnet"
            Integer inputTokens,        // prompt token count
            Integer outputTokens,       // completion token count
            Long    processingTimeMs,   // wall-clock ms from request to response
            String  errorMessage        // populated only when status = ERROR
    ) {}
}