import logging

from bs4 import BeautifulSoup

from src.crawler.base import BaseCrawler
from src.llm import OllamaClient
from src.model import JobCrawlRequestEvent

logger = logging.getLogger(__name__)


class HtmlCrawler(BaseCrawler):
    """HTML 크롤링 후 LLM으로 데이터 추출"""

    def __init__(self):
        super().__init__()
        self.llm_client = OllamaClient()

    def _execute(self, event: JobCrawlRequestEvent) -> dict:
        response = self._http_request(event)
        html = response.text

        # HTML 파싱하여 텍스트 추출
        soup = BeautifulSoup(html, "lxml")

        # 불필요한 태그 제거
        for tag in soup(["script", "style", "nav", "footer", "header"]):
            tag.decompose()

        text_content = soup.get_text(separator="\n", strip=True)

        # LLM으로 데이터 추출
        extracted_data = None
        if event.prompt:
            extracted_data = self.llm_client.extract_json(text_content, event.prompt)
            logger.info(f"Extracted data for jobId: {event.job_id}")

        return {
            "status_code": response.status_code,
            "response_body": text_content[:10000],  # 응답 본문 크기 제한
            "extracted_data": extracted_data,
        }
