package com.seed_crawler.core.dto;

import com.seed_crawler.core.entity.enums.HttpMethod;
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
        private HttpMethod httpMethod;
        private ScheduleType scheduleType;
        private String schedule;
        private String prompt;
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

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String title;
        private String description;
        private String targetUrl;
        private JobExecutionType jobExecutionType;
        private HttpMethod httpMethod;
        private ScheduleType scheduleType;
        private String schedule;
        private String prompt;
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
    public static class UpdateResult {
        private UUID jobId;
        private String title;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateResponse {
        private UUID jobId;
        private String title;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeleteResult {
        private UUID jobId;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeleteResponse {
        private UUID jobId;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StatusRequest {
        private boolean enabled;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusResult {
        private UUID jobId;
        private boolean enabled;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusResponse {
        private UUID jobId;
        private boolean enabled;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OperationResult {
        private UUID jobId;
        private String title;
        private String status;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OperationResponse {
        private UUID jobId;
        private String title;
        private String status;
        private String message;
    }
}
