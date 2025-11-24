import logging
import time
from abc import ABC, abstractmethod
from datetime import datetime

import requests

from src.config import get_settings
from src.model import JobCrawlRequestEvent, JobCrawlResultEvent, CrawlStatus

logger = logging.getLogger(__name__)


class BaseCrawler(ABC):
    def __init__(self):
        self.settings = get_settings()
        self.session = requests.Session()
        self.session.headers.update({"User-Agent": self.settings.crawler.user_agent})

    def crawl(self, event: JobCrawlRequestEvent) -> JobCrawlResultEvent:
        retry_count = 0
        last_error = None

        while retry_count <= event.retry_limit:
            if retry_count > 0:
                logger.info(f"Retrying ({retry_count}/{event.retry_limit}) for jobId: {event.job_id}")
                time.sleep(event.retry_interval_sec)

            try:
                result = self._execute(event)
                return JobCrawlResultEvent(
                    job_id=event.job_id,
                    status=CrawlStatus.SUCCESS,
                    status_code=result.get("status_code"),
                    response_body=result.get("response_body"),
                    extracted_data=result.get("extracted_data"),
                    retry_count=retry_count,
                    crawled_at=datetime.now(),
                )
            except requests.Timeout:
                last_error = "Request timed out"
                logger.warning(f"Timeout for jobId: {event.job_id}, attempt: {retry_count + 1}")
            except Exception as e:
                last_error = str(e)
                logger.warning(f"Failed for jobId: {event.job_id}, attempt: {retry_count + 1}, error: {e}")

            retry_count += 1

        return JobCrawlResultEvent(
            job_id=event.job_id,
            status=CrawlStatus.TIMEOUT if "timed out" in (last_error or "") else CrawlStatus.FAILED,
            error_message=last_error,
            retry_count=retry_count - 1,
            crawled_at=datetime.now(),
        )

    @abstractmethod
    def _execute(self, event: JobCrawlRequestEvent) -> dict:
        """Execute crawling and return result dict with keys: status_code, response_body, extracted_data"""
        pass

    def _http_request(self, event: JobCrawlRequestEvent) -> requests.Response:
        headers = dict(event.header_parameters) if event.header_parameters else {}
        params = dict(event.query_parameters) if event.query_parameters else None
        json_body = dict(event.body_parameters) if event.body_parameters else None

        method = event.http_method.value
        response = self.session.request(
            method=method,
            url=event.target_url,
            headers=headers,
            params=params,
            json=json_body if method in ["POST", "PUT"] else None,
            timeout=event.timeout_sec,
        )
        response.raise_for_status()
        return response
