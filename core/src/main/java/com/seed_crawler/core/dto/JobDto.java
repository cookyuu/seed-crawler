package com.seed_crawler.core.dto;

import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.ScheduleType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

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
        @Enumerated(EnumType.STRING)
        private JobExecutionType jobExecutionType;
        @Enumerated(EnumType.STRING)
        private ScheduleType scheduleType;
        private String schedule;

        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private Map<String, Object> headerParameters;
        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private Map<String, Object> queryParameters;
        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
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
