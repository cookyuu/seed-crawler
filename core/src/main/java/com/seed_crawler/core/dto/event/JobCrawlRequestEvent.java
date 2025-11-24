package com.seed_crawler.core.dto.event;

import com.seed_crawler.core.entity.enums.HttpMethod;
import com.seed_crawler.core.entity.enums.JobExecutionType;
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
public class JobCrawlRequestEvent {
    private UUID jobId;
    private String targetUrl;
    private JobExecutionType jobExecutionType;
    private HttpMethod httpMethod;
    private Map<String, Object> headerParameters;
    private Map<String, Object> queryParameters;
    private Map<String, Object> bodyParameters;
    private String prompt;
    private int timeoutSec;
    private int retryLimit;
    private int retryIntervalSec;
    private String callbackUrl;
    private LocalDateTime requestedAt;
}
