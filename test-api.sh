#!/bin/bash

# Script para probar los endpoints de la API

API_URL="http://localhost:8080/api/ai"

echo "=== Testing FinWhisper API ==="
echo ""

# Test 1: Chat Síncrono
echo "📝 Test 1: Chat Síncrono"
echo "---"
curl -X POST "$API_URL/chat" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Analiza brevemente el estado actual de AAPL. ¿Está sobrecomprado o sobrevendido?"}' \
  -s | jq . 2>/dev/null || echo "No se pudo parsear JSON"
echo ""
echo ""

# Test 2: Chat Streaming
echo "📡 Test 2: Chat Streaming"
echo "---"
echo "Respuesta en streaming:"
curl -X POST "$API_URL/chat-stream" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Dame el RSI actual de TSLA y dame tu análisis"}' \
  -N 2>/dev/null
echo ""
echo ""

echo "✅ Tests completados"

