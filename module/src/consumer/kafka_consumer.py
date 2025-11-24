import json
import logging
from typing import Callable

from kafka import KafkaConsumer

from src.config import get_settings
from src.model import JobCrawlRequestEvent

logger = logging.getLogger(__name__)


class CrawlRequestConsumer:
    def __init__(self):
        self.settings = get_settings()
        self.consumer = None

    def connect(self) -> None:
        kafka_config = self.settings.kafka
        self.consumer = KafkaConsumer(
            kafka_config.request_topic,
            bootstrap_servers=kafka_config.bootstrap_servers,
            group_id=kafka_config.group_id,
            auto_offset_reset=kafka_config.auto_offset_reset,
            value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        )
        logger.info(
            f"Connected to Kafka - bootstrap: {kafka_config.bootstrap_servers}, "
            f"topic: {kafka_config.request_topic}, group: {kafka_config.group_id}"
        )

    def consume(self, handler: Callable[[JobCrawlRequestEvent], None]) -> None:
        if not self.consumer:
            self.connect()

        logger.info("Starting to consume crawl request messages...")
        for message in self.consumer:
            try:
                event = JobCrawlRequestEvent.model_validate(message.value)
                logger.info(f"Received crawl request - jobId: {event.job_id}, url: {event.target_url}")
                handler(event)
            except Exception as e:
                logger.error(f"Error processing message: {e}", exc_info=True)

    def close(self) -> None:
        if self.consumer:
            self.consumer.close()
            logger.info("Kafka consumer closed")
