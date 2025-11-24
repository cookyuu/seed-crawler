from functools import lru_cache

from pydantic_settings import BaseSettings


class KafkaSettings(BaseSettings):
    bootstrap_servers: str = "localhost:9092"
    group_id: str = "crawler-group"
    topic: str = "crawl-events"
    auto_offset_reset: str = "earliest"

    class Config:
        env_prefix = "KAFKA_"


class CrawlerSettings(BaseSettings):
    request_timeout: int = 30
    max_retries: int = 3
    user_agent: str = "SeedCrawler/1.0"

    class Config:
        env_prefix = "CRAWLER_"


class Settings(BaseSettings):
    app_name: str = "seed-crawler-module"
    debug: bool = False
    kafka: KafkaSettings = KafkaSettings()
    crawler: CrawlerSettings = CrawlerSettings()

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache
def get_settings() -> Settings:
    return Settings()
