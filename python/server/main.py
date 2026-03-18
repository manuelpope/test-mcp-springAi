from mcp.server.fastmcp import FastMCP
import numpy as np
import yfinance as yf

mcp = FastMCP("quant-tools")


def calculate_rsi(prices, period=14):
    deltas = np.diff(prices)
    up = np.where(deltas > 0, deltas, 0)
    down = np.where(deltas < 0, -deltas, 0)

    # Usamos una media simple para el ejemplo (NumPy puro)
    avg_gain = np.mean(up[:period])
    avg_loss = np.mean(down[:period])

    for i in range(period, len(deltas)):
        avg_gain = (avg_gain * (period - 1) + up[i]) / period
        avg_loss = (avg_loss * (period - 1) + down[i]) / period

    rs = avg_gain / (avg_loss + 1e-10) # Evitar división por cero
    return 100 - (100 / (1 + rs))

@mcp.tool()
def get_advanced_stats(ticker: str) -> dict:
    stock = yf.Ticker(ticker)
    hist = stock.history(period="6mo") # Necesitamos histórico para la media de 20 días

    closes = hist['Close'].values
    last_20_days = closes[-20:]

    # Cálculos Bollinger con NumPy
    sma_20 = np.mean(last_20_days)
    std_20 = np.std(last_20_days)
    upper_band = sma_20 + (2 * std_20)
    lower_band = sma_20 - (2 * std_20)

    return {
        "ticker": ticker,
        "precio_actual": float(closes[-1]),
        "rsi": float(calculate_rsi(closes)),
        "bollinger_superior": float(upper_band),
        "bollinger_inferior": float(lower_band),
        "media_20": float(sma_20),
        "volatilidad_anual": float(np.std(closes) * np.sqrt(252)) # Volatilidad anualizada
    }
if __name__ == "__main__":
    print("Starting MCP server on SSE mode...")
    mcp.run(transport="sse")

