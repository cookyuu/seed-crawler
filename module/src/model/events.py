from datetime import datetime
from enum import Enum
from typing import Any
from uuid import UUID

from pydantic import BaseModel, Field


class JobExecutionType(str, Enum):
    HTML = "HTML"
    API_JSON = "API_JSON"
    API_XML = "API_XML"
    BROWSER = "BROWSER"
    FILE_DOWNLOAD = "FILE_DOWNLOAD"


class HttpMethod(str, Enum):
    GET = "GET"
    POST = "POST"
    PUT = "PUT"
    DELETE = "DELETE"


class CrawlStatus(str, Enum):
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    TIMEOUT = "TIMEOUT"


class JobCrawlRequestEvent(BaseModel):
    job_id: UUID = Field(alias="jobId")
    target_url: str = Field(alias="targetUrl")
    job_execution_type: JobExecutionType = Field(alias="jobExecutionType")
    http_method: HttpMethod = Field(default=HttpMethod.GET, alias="httpMethod")
    header_parameters: dict[str, Any] | None = Field(default=None, alias="headerParameters")
    query_parameters: dict[str, Any] | None = Field(default=None, alias="queryParameters")
    body_parameters: dict[str, Any] | None = Field(default=None, alias="bodyParameters")
    prompt: str | None = Field(default=None, alias="prompt")
    timeout_sec: int = Field(alias="timeoutSec")
    retry_limit: int = Field(alias="retryLimit")
    retry_interval_sec: int = Field(alias="retryIntervalSec")
    callback_url: str | None = Field(default=None, alias="callbackUrl")
    requested_at: datetime = Field(alias="requestedAt")

    class Config:
        populate_by_name = True


class JobCrawlResultEvent(BaseModel):
    job_id: UUID = Field(alias="jobId")
    status: CrawlStatus
    status_code: int | None = Field(default=None, alias="statusCode")
    response_body: str | None = Field(default=None, alias="responseBody")
    extracted_data: dict[str, Any] | None = Field(default=None, alias="extractedData")
    error_message: str | None = Field(default=None, alias="errorMessage")
    retry_count: int = Field(default=0, alias="retryCount")
    crawled_at: datetime = Field(alias="crawledAt")

    class Config:
        populate_by_name = True

    def to_kafka_dict(self) -> dict:
        return {
            "jobId": str(self.job_id),
            "status": self.status.value,
            "statusCode": self.status_code,
            "responseBody": self.response_body,
            "extractedData": self.extracted_data,
            "errorMessage": self.error_message,
            "retryCount": self.retry_count,
            "crawledAt": self.crawled_at.isoformat(),
        }
