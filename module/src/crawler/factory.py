import logging

from src.crawler.api_json_crawler import ApiJsonCrawler
from src.crawler.api_xml_crawler import ApiXmlCrawler
from src.crawler.base import BaseCrawler
from src.crawler.browser_crawler import BrowserCrawler
from src.crawler.file_crawler import FileCrawler
from src.crawler.html_crawler import HtmlCrawler
from src.model import JobExecutionType

logger = logging.getLogger(__name__)


class CrawlerFactory:
    _crawlers: dict[JobExecutionType, BaseCrawler] = {}

    @classmethod
    def get_crawler(cls, execution_type: JobExecutionType) -> BaseCrawler:
        if execution_type not in cls._crawlers:
            cls._crawlers[execution_type] = cls._create_crawler(execution_type)

        return cls._crawlers[execution_type]

    @classmethod
    def _create_crawler(cls, execution_type: JobExecutionType) -> BaseCrawler:
        crawler_map = {
            JobExecutionType.HTML: HtmlCrawler,
            JobExecutionType.API_JSON: ApiJsonCrawler,
            JobExecutionType.API_XML: ApiXmlCrawler,
            JobExecutionType.BROWSER: BrowserCrawler,
            JobExecutionType.FILE_DOWNLOAD: FileCrawler,
        }

        crawler_class = crawler_map.get(execution_type)
        if not crawler_class:
            raise ValueError(f"Unknown execution type: {execution_type}")

        logger.info(f"Created crawler for type: {execution_type.value}")
        return crawler_class()
