import logging
import signal
import sys

from src.config import get_settings
from src.consumer import KafkaEventConsumer
from src.crawler import WebCrawler

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


def main():
    settings = get_settings()
    logger.info(f"Starting {settings.app_name}")

    consumer = KafkaEventConsumer()
    crawler = WebCrawler()

    def signal_handler(sig, frame):
        logger.info("Shutting down...")
        consumer.close()
        sys.exit(0)

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    def handle_event(event: dict):
        url = event.get("url")
        if not url:
            logger.warning("Received event without URL")
            return

        logger.info(f"Crawling: {url}")
        result = crawler.crawl(url)
        if result:
            logger.info(f"Crawled successfully: {result['title']}")

    try:
        consumer.consume(handle_event)
    except Exception as e:
        logger.error(f"Error: {e}")
        consumer.close()


if __name__ == "__main__":
    main()
