import json
import logging

from kafka import KafkaProducer

from src.config import get_settings
from src.model import JobCrawlResultEvent

logger = logging.getLogger(__name__)


class CrawlResultProducer:
    def __init__(self):
        self.settings = get_settings()
        self.producer = None

    def connect(self) -> None:
        kafka_config = self.settings.kafka
        self.producer = KafkaProducer(
            bootstrap_servers=kafka_config.bootstrap_servers,
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
            key_serializer=lambda k: k.encode("utf-8") if k else None,
        )
        logger.info(
            f"Kafka producer connected - bootstrap: {kafka_config.bootstrap_servers}, "
            f"topic: {kafka_config.result_topic}"
        )

    def send(self, result: JobCrawlResultEvent) -> None:
        if not self.producer:
            self.connect()

        topic = self.settings.kafka.result_topic
        key = str(result.job_id)
        value = result.to_kafka_dict()

        future = self.producer.send(topic, key=key, value=value)
        future.add_callback(
            lambda metadata: logger.info(
                f"Sent crawl result - jobId: {result.job_id}, "
                f"status: {result.status.value}, offset: {metadata.offset}"
            )
        )
        future.add_errback(
            lambda e: logger.error(f"Failed to send crawl result for jobId: {result.job_id}, error: {e}")
        )

    def flush(self) -> None:
        if self.producer:
            self.producer.flush()

    def close(self) -> None:
        if self.producer:
            self.producer.flush()
            self.producer.close()
            logger.info("Kafka producer closed")
