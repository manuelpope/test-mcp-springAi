import requests
import yfinance as yf
import logging
from xml.etree import ElementTree

logger = logging.getLogger(__name__)


def _get_rss_news(ticker: str, limit: int) -> list:
    """Obtiene noticias desde Google News RSS."""
    try:
        search_url = f"https://news.google.com/rss/search?q={ticker}&hl=en-US"
        logger.info(f"RSS: {search_url}")

        response = requests.get(search_url, timeout=10)
        response.raise_for_status()

        root = ElementTree.fromstring(response.content)
        news_items = []

        for item in root.findall(".//item")[:limit]:
            title = item.findtext("title", "N/A")
            link = item.findtext("link", "N/A")
            pub_date = item.findtext("pubDate", "N/A")
            source = item.findtext("source", "Unknown")

            news_items.append(
                {
                    "titulo": title,
                    "enlace": link,
                    "fuente": source,
                    "tiempo": pub_date,
                    "origen": "Google News RSS",
                }
            )

        logger.info(f"RSS: {len(news_items)} noticias encontradas")
        return news_items

    except Exception as e:
        logger.warning(f"RSS: Error - {str(e)}")
        return []


def _get_yahoo_news(ticker: str, limit: int) -> list:
    """Obtiene noticias desde Yahoo Finance."""
    try:
        stock = yf.Ticker(ticker)
        news = stock.news

        if not news:
            logger.info("Yahoo: Sin noticias")
            return []

        news_items = []
        for item in news[:limit]:
            try:
                content = item.get("content", {})
                news_items.append(
                    {
                        "titulo": content.get("title", "N/A"),
                        "enlace": content.get("clickUrl", "N/A"),
                        "fuente": content.get("provider", {}).get(
                            "name", "Yahoo Finance"
                        ),
                        "tiempo": content.get("pubDate", "N/A"),
                        "origen": "Yahoo Finance",
                    }
                )
            except Exception as e:
                logger.warning(f"Yahoo: Error procesando item - {str(e)}")
                continue

        logger.info(f"Yahoo: {len(news_items)} noticias encontradas")
        return news_items

    except Exception as e:
        logger.warning(f"Yahoo: Error - {str(e)}")
        return []


def get_news_headlines(ticker: str, limit: int = 10) -> dict:
    """
    Busca noticias sobre un ticker combinando Google News RSS y Yahoo Finance.

    Args:
        ticker: Símbolo del ticker (ej: AAPL, MSFT)
        limit: Número máximo de noticias por fuente (default: 10)

    Returns:
        Dict con lista de noticias combinadas
    """
    logger.info(f"Buscando noticias para {ticker}...")

    rss_news = _get_rss_news(ticker, limit)
    yahoo_news = _get_yahoo_news(ticker, limit)

    all_news = rss_news + yahoo_news

    if not all_news:
        return {
            "ticker": ticker,
            "total": 0,
            "noticias": [],
            "aviso": "No se encontraron noticias.",
        }

    logger.info(f"Total: {len(all_news)} noticias para {ticker}")

    return {
        "ticker": ticker,
        "total": len(all_news),
        "noticias": all_news,
        "fuentes": {"google_rss": len(rss_news), "yahoo_finance": len(yahoo_news)},
    }
