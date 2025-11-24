import logging
import signal
import sys

from src.config import get_settings
from src.consumer import CrawlRequestConsumer
from src.crawler import WebCrawler
from src.model import JobCrawlRequestEvent
from src.producer import CrawlResultProducer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


class CrawlerApplication:
    def __init__(self):
        self.settings = get_settings()
        self.consumer = CrawlRequestConsumer()
        self.producer = CrawlResultProducer()
        self.crawler = WebCrawler()
        self.running = True

    def handle_event(self, event: JobCrawlRequestEvent) -> None:
        logger.info(f"Processing crawl request - jobId: {event.job_id}, url: {event.target_url}")

        result = self.crawler.crawl(event)
        self.producer.send(result)

        logger.info(f"Crawl completed - jobId: {event.job_id}, status: {result.status.value}")

    def shutdown(self) -> None:
        logger.info("Shutting down...")
        self.running = False
        self.producer.close()
        self.consumer.close()

    def run(self) -> None:
        logger.info(f"Starting {self.settings.app_name}")

        def signal_handler(sig, frame):
            self.shutdown()
            sys.exit(0)

        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)

        try:
            self.consumer.consume(self.handle_event)
        except Exception as e:
            logger.error(f"Error: {e}", exc_info=True)
            self.shutdown()


def main():
    app = CrawlerApplication()
    app.run()


if __name__ == "__main__":
    main()
