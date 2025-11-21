package com.seed_crawler.core.dto.command;

import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class JobCreationCommand {
    private final UUID memberId;
    private final String title;
    private final String description;
    private final String targetUrl;
    private final JobExecutionType jobExecutionType;
    private final ScheduleType scheduleType;
    private final String schedule;
    private final Map<String, Object> headerParameters;
    private final Map<String, Object> bodyParameters;
    private final Map<String, Object> queryParameters;
    private final Integer retryLimit;
    private final Integer retryIntervalSec;
    private final Integer timeoutSec;
    private final String callbackUrl;
}
