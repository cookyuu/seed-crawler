from functools import lru_cache

from pydantic_settings import BaseSettings


class KafkaSettings(BaseSettings):
    bootstrap_servers: str = "localhost:9092"
    group_id: str = "seed-crawler-module-group"
    request_topic: str = "job-crawl-request"
    result_topic: str = "job-crawl-result"
    auto_offset_reset: str = "earliest"

    class Config:
        env_prefix = "KAFKA_"


class CrawlerSettings(BaseSettings):
    default_timeout_sec: int = 30
    default_retry_limit: int = 3
    default_retry_interval_sec: int = 60
    user_agent: str = "SeedCrawler/1.0"

    class Config:
        env_prefix = "CRAWLER_"


class OllamaSettings(BaseSettings):
    base_url: str = "http://localhost:11434"
    model: str = "llama3.2"
    timeout_sec: int = 120

    class Config:
        env_prefix = "OLLAMA_"


class SeleniumSettings(BaseSettings):
    headless: bool = True
    page_load_timeout: int = 30

    class Config:
        env_prefix = "SELENIUM_"


class Settings(BaseSettings):
    app_name: str = "seed-crawler-module"
    debug: bool = False
    kafka: KafkaSettings = KafkaSettings()
    crawler: CrawlerSettings = CrawlerSettings()
    ollama: OllamaSettings = OllamaSettings()
    selenium: SeleniumSettings = SeleniumSettings()

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache
def get_settings() -> Settings:
    return Settings()
