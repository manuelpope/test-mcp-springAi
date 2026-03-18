#!/bin/bash

# Script para iniciar toda la aplicación

echo "=== FinWhisper - Full Stack Startup ==="
echo ""

# Verificar que Ollama esté corriendo
echo "1. Verificando Ollama..."
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "❌ ERROR: Ollama no está corriendo"
    echo "   Inicia Ollama con: ollama serve"
    exit 1
fi
echo "✅ Ollama está corriendo"

# Asegurarse que el modelo está disponible
echo ""
echo "2. Verificando modelo llama3.1:8b..."
if curl -s http://localhost:11434/api/tags | grep -q '"name":"llama3.1:8b"'; then
    echo "✅ Modelo llama3.1:8b disponible"
else
    echo "⚠️  llama3.1:8b no está descargado. Descargándolo..."
    ollama pull llama3.1:8b
fi

# Compilar Spring Boot
echo ""
echo "3. Compilando Spring Boot..."
mvn clean install -q
if [ $? -ne 0 ]; then
    echo "❌ ERROR en compilación de Maven"
    exit 1
fi
echo "✅ Spring Boot compilado"

# Iniciar la aplicación
echo ""
echo "=== Iniciando FinWhisper ==="
echo "📝 Servidor disponible en: http://localhost:8080"
echo "📊 Endpoints:"
echo "   - POST /api/ai/chat (respuesta síncrona)"
echo "   - POST /api/ai/chat-stream (respuesta en streaming)"
echo ""
mvn spring-boot:run
