package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.ScheduleType;

import java.util.Map;
import java.util.UUID;

public interface JobService {
    JobDto.SaveResult saveJob(UUID memberId, String title, String description, String targetUrl, JobExecutionType jobExecutionType, ScheduleType scheduleType, String schedule, Map<String, Object> headerParameters, Map<String, Object> bodyParameters, Map<String, Object> queryParameters, Integer retryLimit, Integer retryIntervalSec, Integer timeoutSec, String callbackUrl);
}
