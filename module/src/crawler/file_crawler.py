import logging
import os
import tempfile
from urllib.parse import urlparse

from src.crawler.base import BaseCrawler
from src.model import JobCrawlRequestEvent

logger = logging.getLogger(__name__)


class FileCrawler(BaseCrawler):
    """파일 다운로드"""

    def _execute(self, event: JobCrawlRequestEvent) -> dict:
        response = self._http_request(event)

        # 파일명 추출
        parsed_url = urlparse(event.target_url)
        filename = os.path.basename(parsed_url.path) or "downloaded_file"

        # 임시 파일로 저장
        temp_dir = tempfile.gettempdir()
        file_path = os.path.join(temp_dir, f"{event.job_id}_{filename}")

        with open(file_path, "wb") as f:
            f.write(response.content)

        logger.info(f"File downloaded for jobId: {event.job_id}, path: {file_path}")

        return {
            "status_code": response.status_code,
            "response_body": f"File saved: {file_path}",
            "extracted_data": {
                "file_path": file_path,
                "file_size": len(response.content),
                "content_type": response.headers.get("Content-Type"),
            },
        }
