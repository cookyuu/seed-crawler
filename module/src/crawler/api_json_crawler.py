import json
import logging

from src.crawler.base import BaseCrawler
from src.llm import OllamaClient
from src.model import JobCrawlRequestEvent

logger = logging.getLogger(__name__)


class ApiJsonCrawler(BaseCrawler):
    """REST API (JSON) 호출 후 LLM으로 데이터 추출"""

    def __init__(self):
        super().__init__()
        self.llm_client = OllamaClient()

    def _execute(self, event: JobCrawlRequestEvent) -> dict:
        response = self._http_request(event)

        try:
            json_data = response.json()
            json_str = json.dumps(json_data, ensure_ascii=False, indent=2)
        except json.JSONDecodeError:
            json_str = response.text

        # LLM으로 데이터 추출
        extracted_data = None
        if event.prompt:
            extracted_data = self.llm_client.extract_json(json_str, event.prompt)
            logger.info(f"Extracted data for jobId: {event.job_id}")

        return {
            "status_code": response.status_code,
            "response_body": json_str[:10000],
            "extracted_data": extracted_data,
        }
