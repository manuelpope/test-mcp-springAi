# FinWhisper - Financial Analysis with AI & MCP

## Arquitectura

```
┌─────────────────────────────────────────────────┐
│         Spring Boot Application (Port 8080)     │
│  - ChatClient (Ollama Integration)              │
│  - MCP Client (conecta con servidor Python)     │
│  - REST API (/api/ai/chat, /api/ai/chat-stream) │
└──────────────────────┬──────────────────────────┘
                       │ (stdio)
                       ↓
┌─────────────────────────────────────────────────┐
│    Python MCP Server (FastMCP)                  │
│  - get_advanced_stats(ticker)                   │
│  - Usa yfinance para obtener datos              │
│  - Calcula RSI, volatilidad, promedios          │
└─────────────────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────┐
│    Ollama (Local LLM on Port 11434)             │
│  - Modelo: llama3.1:8b                               │
│  - Recibe prompts de Spring AI                  │
│  - Usa herramientas MCP para análisis           │
└─────────────────────────────────────────────────┘
```

## Setup & Ejecución

### Requisitos
- Java 21+
- Python 3.8+
- Ollama corriendo en `http://localhost:11434`
- El modelo `llama3.1:8b` descargado en Ollama

### Paso 1: Iniciar Ollama
```bash
# Instalar Ollama desde https://ollama.ai
# Luego ejecutar:
ollama serve
# En otra terminal, asegurar que el modelo está disponible:
ollama pull llama3.1:8b
```

### Paso 2: Iniciar el servidor MCP de Python
```bash
cd /Volumes/jetdrive/LLMAdvisors/spring/finwhisper
python python/run_server.py
```
Este script automáticamente:
- Crea un venv si no existe
- Instala dependencias (numpy, yfinance, mcp)
- Inicia el servidor MCP en modo stdio

### Paso 3: Iniciar Spring Boot
```bash
cd /Volumes/jetdrive/LLMAdvisors/spring/finwhisper
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`

## API Endpoints

### Chat Síncrono
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Analiza el precio de AAPL y dame tu opinión"}'
```

### Chat Streaming (para respuestas en tiempo real)
```bash
curl -X POST http://localhost:8080/api/ai/chat-stream \
  -H "Content-Type: application/json" \
  -d '{"prompt":"¿Cuál es el RSI actual de AAPL? ¿Está sobrecomprado?"}'
```

## Cómo funciona

1. Envías un prompt al endpoint `/api/ai/chat`
2. Spring AI recibe el prompt y lo envía a Ollama (llama3.1:8b)
3. Si el prompt menciona análisis de acciones, Ollama reconoce que necesita herramientas
4. Ollama hace una llamada MCP al servidor Python para ejecutar `get_advanced_stats(ticker)`
5. El servidor Python obtiene datos de yfinance y calcula indicadores
6. Ollama recibe los datos y genera una respuesta contextualizada
7. La respuesta se devuelve al cliente

## Notas Importantes

- **Configuración MCP**: El cliente MCP de Spring AI se configura en `application.yaml` para ejecutar `python/run_server.py` via stdio
- **El servidor Python debe estar corriendo**: Spring Boot lo ejecutará automáticamente, pero asegúrate de que el venv se crea correctamente
- **Modelo Ollama**: Cambia `llama3.1:8b` en `application.yaml` si tienes otro modelo disponible
- **Errores comunes**:
  - "Connection refused" a Ollama → Asegúrate de que Ollama esté corriendo
  - ModuleNotFoundError (Python) → El venv no se creó, intenta manualmente: `python -m venv python/venv && source python/venv/bin/activate && pip install -r python/requirements.txt`
