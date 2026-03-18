#!/usr/bin/env python3
"""
Cliente Python para probar la API de FinWhisper
Uso: python test-client.py
"""

import requests
import json
import time

API_BASE = "http://localhost:8080/api/ai"

def test_sync_chat(prompt):
    """Prueba el endpoint de chat síncrono"""
    print(f"\n📝 Enviando: {prompt}")
    print("-" * 60)

    try:
        response = requests.post(
            f"{API_BASE}/chat",
            json={"prompt": prompt},
            timeout=30
        )

        if response.status_code == 200:
            print("✅ Respuesta:")
            print(response.text)
        else:
            print(f"❌ Error: {response.status_code}")
            print(response.text)
    except requests.exceptions.ConnectionError:
        print("❌ Error: No se puede conectar a la API")
        print("   Asegúrate que Spring Boot está corriendo en http://localhost:8080")
    except Exception as e:
        print(f"❌ Error: {str(e)}")

def test_streaming_chat(prompt):
    """Prueba el endpoint de chat con streaming"""
    print(f"\n📡 Enviando (streaming): {prompt}")
    print("-" * 60)

    try:
        response = requests.post(
            f"{API_BASE}/chat-stream",
            json={"prompt": prompt},
            stream=True,
            timeout=60
        )

        if response.status_code == 200:
            print("✅ Respuesta en tiempo real:")
            for chunk in response.iter_content(decode_unicode=True):
                if chunk:
                    print(chunk, end='', flush=True)
            print("\n")
        else:
            print(f"❌ Error: {response.status_code}")
            print(response.text)
    except requests.exceptions.ConnectionError:
        print("❌ Error: No se puede conectar a la API")
        print("   Asegúrate que Spring Boot está corriendo")
    except Exception as e:
        print(f"❌ Error: {str(e)}")

def main():
    print("=" * 60)
    print("FinWhisper - Cliente de Prueba")
    print("=" * 60)

    # Test cases
    test_cases = [
        {
            "name": "Análisis simple de acción",
            "prompt": "¿Cuál es el precio actual de AAPL?",
            "type": "sync"
        },
        {
            "name": "Análisis técnico",
            "prompt": "Analiza el RSI de MSFT. ¿Está sobrecomprado o sobrevendido?",
            "type": "sync"
        },
        {
            "name": "Recomendación de inversión",
            "prompt": "Compara TSLA y NVDA. Basándote en volatilidad y RSI, ¿cuál tiene mejor oportunidad?",
            "type": "streaming"
        },
    ]

    for i, test in enumerate(test_cases, 1):
        print(f"\n\n{'=' * 60}")
        print(f"Test {i}: {test['name']}")
        print(f"{'=' * 60}")

        if test['type'] == 'sync':
            test_sync_chat(test['prompt'])
        else:
            test_streaming_chat(test['prompt'])

        time.sleep(2)  # Pequeña pausa entre requests

    print("\n" + "=" * 60)
    print("✅ Pruebas completadas")
    print("=" * 60)

if __name__ == "__main__":
    # Verifica que requests está instalado
    try:
        import requests
    except ImportError:
        print("❌ Error: requests no está instalado")
        print("   Instala con: pip install requests")
        exit(1)

    main()

