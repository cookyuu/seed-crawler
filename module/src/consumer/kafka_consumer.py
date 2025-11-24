import json
import logging
from typing import Callable

from kafka import KafkaConsumer

from src.config import get_settings

logger = logging.getLogger(__name__)


class KafkaEventConsumer:
    def __init__(self):
        self.settings = get_settings()
        self.consumer = None

    def connect(self) -> None:
        kafka_config = self.settings.kafka
        self.consumer = KafkaConsumer(
            kafka_config.topic,
            bootstrap_servers=kafka_config.bootstrap_servers,
            group_id=kafka_config.group_id,
            auto_offset_reset=kafka_config.auto_offset_reset,
            value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        )
        logger.info(f"Connected to Kafka topic: {kafka_config.topic}")

    def consume(self, handler: Callable[[dict], None]) -> None:
        if not self.consumer:
            self.connect()

        logger.info("Starting to consume messages...")
        for message in self.consumer:
            try:
                handler(message.value)
            except Exception as e:
                logger.error(f"Error processing message: {e}")

    def close(self) -> None:
        if self.consumer:
            self.consumer.close()
            logger.info("Kafka consumer closed")
