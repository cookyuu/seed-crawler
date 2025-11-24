import logging

import requests
from bs4 import BeautifulSoup

from src.config import get_settings

logger = logging.getLogger(__name__)


class WebCrawler:
    def __init__(self):
        self.settings = get_settings()
        self.session = requests.Session()
        self.session.headers.update({"User-Agent": self.settings.crawler.user_agent})

    def fetch(self, url: str) -> str | None:
        try:
            response = self.session.get(
                url,
                timeout=self.settings.crawler.request_timeout,
            )
            response.raise_for_status()
            return response.text
        except requests.RequestException as e:
            logger.error(f"Failed to fetch {url}: {e}")
            return None

    def parse(self, html: str) -> BeautifulSoup:
        return BeautifulSoup(html, "lxml")

    def crawl(self, url: str) -> dict | None:
        html = self.fetch(url)
        if not html:
            return None

        soup = self.parse(html)
        return {
            "url": url,
            "title": soup.title.string if soup.title else None,
            "content": soup.get_text(strip=True),
        }
