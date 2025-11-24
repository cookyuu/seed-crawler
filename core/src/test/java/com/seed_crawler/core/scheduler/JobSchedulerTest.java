package com.seed_crawler.core.scheduler;

import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.JobStatus;
import com.seed_crawler.core.entity.enums.ScheduleType;
import com.seed_crawler.core.repository.JobRepository;
import com.seed_crawler.core.service.JobEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobSchedulerTest {

    @Mock
    JobRepository jobRepository;

    @Mock
    JobEventProducer jobEventProducer;

    @InjectMocks
    JobScheduler jobScheduler;

    private Job createTestJob(UUID jobId, String title, ScheduleType scheduleType, Integer intervalSec) {
        Member member = new Member();
        ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

        Job job = Job.builder()
                .title(title)
                .description("test description")
                .targetUrl("https://example.com")
                .scheduleType(scheduleType)
                .intervalSec(intervalSec)
                .jobExecutionType(JobExecutionType.API_JSON)
                .retryLimit(3)
                .retryIntervalSec(60)
                .timeoutSec(30)
                .member(member)
                .build();

        ReflectionTestUtils.setField(job, "id", jobId);
        ReflectionTestUtils.setField(job, "enabled", true);
        ReflectionTestUtils.setField(job, "status", JobStatus.SCHEDULED);
        ReflectionTestUtils.setField(job, "nextRunAt", LocalDateTime.now().minusMinutes(1));

        return job;
    }

    @Test
    @DisplayName("성공: 스케줄된 Job이 있으면 Kafka 이벤트 발행")
    void processScheduledJobs_success() {
        // Given
        UUID jobId = UUID.randomUUID();
        Job job = createTestJob(jobId, "Test Job", ScheduleType.INTERVAL, 300);

        when(jobRepository.findJobsReadyToRun(eq(JobStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        jobScheduler.processScheduledJobs();

        // Then
        verify(jobEventProducer, times(1)).sendCrawlRequest(job);
        verify(jobRepository, times(2)).save(any(Job.class));

        // Job 상태 변경 확인
        assertThat(job.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(job.getNextRunAt()).isNotNull();
        assertThat(job.getLastRunAt()).isNotNull();
    }

    @Test
    @DisplayName("성공: 스케줄된 Job이 없으면 아무 작업도 하지 않음")
    void processScheduledJobs_noJobsToRun() {
        // Given
        when(jobRepository.findJobsReadyToRun(eq(JobStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        jobScheduler.processScheduledJobs();

        // Then
        verify(jobEventProducer, never()).sendCrawlRequest(any());
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("성공: 여러 Job이 스케줄되어 있으면 모두 처리")
    void processScheduledJobs_multipleJobs() {
        // Given
        Job job1 = createTestJob(UUID.randomUUID(), "Job 1", ScheduleType.INTERVAL, 300);
        Job job2 = createTestJob(UUID.randomUUID(), "Job 2", ScheduleType.INTERVAL, 600);
        Job job3 = createTestJob(UUID.randomUUID(), "Job 3", ScheduleType.INTERVAL, 900);

        when(jobRepository.findJobsReadyToRun(eq(JobStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(job1, job2, job3));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        jobScheduler.processScheduledJobs();

        // Then
        verify(jobEventProducer, times(3)).sendCrawlRequest(any(Job.class));
        verify(jobRepository, times(6)).save(any(Job.class));
    }

    @Test
    @DisplayName("성공: INTERVAL Job 처리 후 nextRunAt이 intervalSec만큼 증가")
    void processScheduledJobs_intervalJobNextRunAtUpdated() {
        // Given
        int intervalSec = 300;
        Job job = createTestJob(UUID.randomUUID(), "Interval Job", ScheduleType.INTERVAL, intervalSec);
        LocalDateTime beforeNextRunAt = job.getNextRunAt();

        when(jobRepository.findJobsReadyToRun(eq(JobStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        jobScheduler.processScheduledJobs();

        // Then
        assertThat(job.getNextRunAt()).isAfter(beforeNextRunAt);
    }

    @Test
    @DisplayName("실패: 이벤트 발행 중 예외 발생 시 다른 Job은 계속 처리")
    void processScheduledJobs_exceptionHandling() {
        // Given
        Job job1 = createTestJob(UUID.randomUUID(), "Job 1", ScheduleType.INTERVAL, 300);
        Job job2 = createTestJob(UUID.randomUUID(), "Job 2", ScheduleType.INTERVAL, 600);

        when(jobRepository.findJobsReadyToRun(eq(JobStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(job1, job2));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("Kafka error")).when(jobEventProducer).sendCrawlRequest(job1);
        doNothing().when(jobEventProducer).sendCrawlRequest(job2);

        // When
        jobScheduler.processScheduledJobs();

        // Then
        verify(jobEventProducer, times(1)).sendCrawlRequest(job1);
        verify(jobEventProducer, times(1)).sendCrawlRequest(job2);
    }
}
