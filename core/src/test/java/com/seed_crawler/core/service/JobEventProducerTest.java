package com.seed_crawler.core.service;

import com.seed_crawler.core.config.KafkaConfig;
import com.seed_crawler.core.dto.event.JobCrawlRequestEvent;
import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.ScheduleType;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobEventProducerTest {

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    JobEventProducer jobEventProducer;

    private Job createTestJob() {
        Member member = new Member();
        ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

        Job job = Job.builder()
                .title("Test Job")
                .description("Test Description")
                .targetUrl("https://example.com/api")
                .scheduleType(ScheduleType.INTERVAL)
                .intervalSec(300)
                .jobExecutionType(JobExecutionType.API_JSON)
                .headerParameters(Map.of("Authorization", "Bearer token"))
                .queryParameters(Map.of("page", 1))
                .bodyParameters(Map.of())
                .retryLimit(3)
                .retryIntervalSec(60)
                .timeoutSec(30)
                .callbackUrl("https://callback.example.com")
                .member(member)
                .build();

        ReflectionTestUtils.setField(job, "id", UUID.randomUUID());
        return job;
    }

    @Test
    @DisplayName("성공: Job 정보로 Kafka 이벤트 발행")
    void sendCrawlRequest_success() {
        // Given
        Job job = createTestJob();

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(KafkaConfig.JOB_CRAWL_REQUEST_TOPIC, 0),
                0L, 0, 0L, 0, 0
        );
        SendResult<String, Object> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(KafkaConfig.JOB_CRAWL_REQUEST_TOPIC), eq(job.getId().toString()), any()))
                .thenReturn(future);

        // When
        jobEventProducer.sendCrawlRequest(job);

        // Then
        ArgumentCaptor<JobCrawlRequestEvent> eventCaptor = ArgumentCaptor.forClass(JobCrawlRequestEvent.class);
        verify(kafkaTemplate).send(
                eq(KafkaConfig.JOB_CRAWL_REQUEST_TOPIC),
                eq(job.getId().toString()),
                eventCaptor.capture()
        );

        JobCrawlRequestEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getJobId()).isEqualTo(job.getId());
        assertThat(capturedEvent.getTargetUrl()).isEqualTo(job.getTargetUrl());
        assertThat(capturedEvent.getJobExecutionType()).isEqualTo(job.getJobExecutionType());
        assertThat(capturedEvent.getHeaderParameters()).isEqualTo(job.getHeaderParameters());
        assertThat(capturedEvent.getQueryParameters()).isEqualTo(job.getQueryParameters());
        assertThat(capturedEvent.getTimeoutSec()).isEqualTo(job.getTimeoutSec());
        assertThat(capturedEvent.getRetryLimit()).isEqualTo(job.getRetryLimit());
        assertThat(capturedEvent.getRequestedAt()).isNotNull();
    }

    @Test
    @DisplayName("성공: 토픽 이름이 job-crawl-request로 전송됨")
    void sendCrawlRequest_correctTopic() {
        // Given
        Job job = createTestJob();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // When
        jobEventProducer.sendCrawlRequest(job);

        // Then
        verify(kafkaTemplate).send(
                eq(KafkaConfig.JOB_CRAWL_REQUEST_TOPIC),
                eq(job.getId().toString()),
                any(JobCrawlRequestEvent.class)
        );
    }

    @Test
    @DisplayName("성공: Job ID가 Kafka 메시지 키로 사용됨")
    void sendCrawlRequest_jobIdAsKey() {
        // Given
        Job job = createTestJob();
        UUID expectedJobId = job.getId();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // When
        jobEventProducer.sendCrawlRequest(job);

        // Then
        verify(kafkaTemplate).send(
                anyString(),
                eq(expectedJobId.toString()),
                any()
        );
    }
}
