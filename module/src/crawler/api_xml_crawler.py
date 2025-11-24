import logging

from src.crawler.base import BaseCrawler
from src.llm import OllamaClient
from src.model import JobCrawlRequestEvent

logger = logging.getLogger(__name__)


class ApiXmlCrawler(BaseCrawler):
    """REST API (XML) 호출 후 LLM으로 데이터 추출"""

    def __init__(self):
        super().__init__()
        self.llm_client = OllamaClient()

    def _execute(self, event: JobCrawlRequestEvent) -> dict:
        response = self._http_request(event)
        xml_content = response.text

        # LLM으로 데이터 추출
        extracted_data = None
        if event.prompt:
            extracted_data = self.llm_client.extract_json(xml_content, event.prompt)
            logger.info(f"Extracted data for jobId: {event.job_id}")

        return {
            "status_code": response.status_code,
            "response_body": xml_content[:10000],
            "extracted_data": extracted_data,
        }
