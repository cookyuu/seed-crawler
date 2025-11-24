package com.seed_crawler.core.service;

import com.seed_crawler.core.config.KafkaConfig;
import com.seed_crawler.core.dto.event.JobCrawlRequestEvent;
import com.seed_crawler.core.entity.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendCrawlRequest(Job job) {
        JobCrawlRequestEvent event = JobCrawlRequestEvent.builder()
                .jobId(job.getId())
                .targetUrl(job.getTargetUrl())
                .jobExecutionType(job.getJobExecutionType())
                .httpMethod(job.getHttpMethod())
                .headerParameters(job.getHeaderParameters())
                .queryParameters(job.getQueryParameters())
                .bodyParameters(job.getBodyParameters())
                .prompt(job.getPrompt())
                .timeoutSec(job.getTimeoutSec())
                .retryLimit(job.getRetryLimit())
                .retryIntervalSec(job.getRetryIntervalSec())
                .callbackUrl(job.getCallbackUrl())
                .requestedAt(LocalDateTime.now())
                .build();

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaConfig.JOB_CRAWL_REQUEST_TOPIC,
                job.getId().toString(),
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent crawl request for jobId={}, offset={}",
                        job.getId(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send crawl request for jobId={}", job.getId(), ex);
            }
        });
    }
}
