package com.seed_crawler.core.dto;

import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

public class JobDto {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SaveRequest {
        private String title;
        private String description;
        private String targetUrl;
        private JobExecutionType jobExecutionType;
        private ScheduleType scheduleType;
        private String schedule;
        private Map<String, Object> headerParameters;
        private Map<String, Object> queryParameters;
        private Map<String, Object> bodyParameters;
        private Integer retryLimit;
        private Integer retryIntervalSec;
        private Integer timeoutSec;
        private String callbackUrl;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveResult {
        private UUID memberId;
        private UUID jobId;
        private String title;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveResponse {
        private UUID memberId;
        private UUID jobId;
        private String title;
    }
}
