import logging

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.support.ui import WebDriverWait
from webdriver_manager.chrome import ChromeDriverManager

from src.config import get_settings
from src.crawler.base import BaseCrawler
from src.llm import OllamaClient
from src.model import JobCrawlRequestEvent

logger = logging.getLogger(__name__)


class BrowserCrawler(BaseCrawler):
    """Selenium으로 JS 렌더링 후 LLM으로 데이터 추출"""

    def __init__(self):
        super().__init__()
        self.llm_client = OllamaClient()

    def _get_driver(self) -> webdriver.Chrome:
        settings = get_settings()
        options = Options()

        if settings.selenium.headless:
            options.add_argument("--headless=new")

        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--disable-gpu")
        options.add_argument(f"user-agent={settings.crawler.user_agent}")

        service = Service(ChromeDriverManager().install())
        driver = webdriver.Chrome(service=service, options=options)
        driver.set_page_load_timeout(settings.selenium.page_load_timeout)

        return driver

    def _execute(self, event: JobCrawlRequestEvent) -> dict:
        driver = self._get_driver()

        try:
            driver.get(event.target_url)

            # 페이지 로드 대기
            WebDriverWait(driver, event.timeout_sec).until(
                lambda d: d.execute_script("return document.readyState") == "complete"
            )

            # 페이지 소스 가져오기
            page_source = driver.page_source

            # 텍스트 추출
            text_content = driver.find_element("tag name", "body").text

            # LLM으로 데이터 추출
            extracted_data = None
            if event.prompt:
                extracted_data = self.llm_client.extract_json(text_content, event.prompt)
                logger.info(f"Extracted data for jobId: {event.job_id}")

            return {
                "status_code": 200,
                "response_body": text_content[:10000],
                "extracted_data": extracted_data,
            }
        finally:
            driver.quit()
