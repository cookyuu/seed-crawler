package com.seed_crawler.core.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCrawlResultEvent {
    private UUID jobId;
    private CrawlStatus status;
    private Integer statusCode;
    private String responseBody;
    private Map<String, Object> extractedData;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime crawledAt;

    public enum CrawlStatus {
        SUCCESS,
        FAILED,
        TIMEOUT
    }
}
