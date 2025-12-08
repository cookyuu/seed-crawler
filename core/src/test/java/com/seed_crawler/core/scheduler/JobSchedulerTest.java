package com.seed_crawler.core.scheduler;

import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.JobStatus;
import com.seed_crawler.core.entity.enums.ScheduleType;
import com.seed_crawler.core.repository.JobRepository;
import com.seed_crawler.core.service.JobScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobSchedulerTest {

    @Mock
    JobRepository jobRepository;

    @Mock
    JobScheduleService jobScheduleService;

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
    @DisplayName("성공: 스케줄된 Job이 있으면 각 단계별로 처리됨")
    void processScheduledJobs_success() {
        // Given
        UUID jobId = UUID.randomUUID();
        Job job = createTestJob(jobId, "Test Job", ScheduleType.INTERVAL, 300);

        when(jobRepository.findJobsReadyToRun(eq(JobStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(job));
        when(jobScheduleService.processJob(jobId)).thenReturn(true);

        // When
        jobScheduler.processScheduledJobs();

        // Then
        verify(jobScheduleService, times(1)).processJob(jobId);
        verify(jobScheduleService, times(1)).sendKafkaEvent(job);
        verify(jobScheduleService, times(1)).completeJob(jobId);
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
        verify(jobScheduleService, never()).processJob(any());
        verify(jobScheduleService, never()).sendKafkaEvent(any());
        verify(jobScheduleService, never()).completeJob(any());
    }

    @Test
    @DisplayName("성공: 여러 Job이 스케줄되어 있으면 모두 독립적으로 처리")
    void processScheduledJobs_multipleJobs() {
        // Given
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();
        UUID jobId3 = UUID.randomUUID();

        Job job1 = createTestJob(jobId1, "Job 1", ScheduleType.INTERVAL, 300);
        Job job2 = createTestJob(jobId2, "Job 2", ScheduleType.INTERVAL, 600);
        Job job3 = createTestJob(jobId3, "Job 3", ScheduleType.INTERVAL, 900);

        when(jobRepository.findJobsReadyToRun(eq(JobStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(job1, job2, job3));
        when(jobScheduleService.processJob(any())).thenReturn(true);

        // When
        jobScheduler.processScheduledJobs();

        // Then
        verify(jobScheduleService, times(3)).processJob(any());
        verify(jobScheduleService, times(3)).sendKafkaEvent(any());
        verify(jobScheduleService, times(3)).completeJob(any());
    }

    @Test
    @DisplayName("실패: processJob 실패 시 후속 단계 실행 안함")
    void processScheduledJobs_processJobFails() {
        // Given
        UUID jobId = UUID.randomUUID();
        Job job = createTestJob(jobId, "Test Job", ScheduleType.INTERVAL, 300);

        when(jobRepository.findJobsReadyToRun(eq(JobStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(job));
        when(jobScheduleService.processJob(jobId)).thenReturn(false);

        // When
        jobScheduler.processScheduledJobs();

        // Then
        verify(jobScheduleService, times(1)).processJob(jobId);
        verify(jobScheduleService, never()).sendKafkaEvent(any());
        verify(jobScheduleService, never()).completeJob(any());
    }

    @Test
    @DisplayName("실패: Kafka 이벤트 발행 실패 시 Job 상태를 STOP으로 변경")
    void processScheduledJobs_kafkaEventFails() {
        // Given
        UUID jobId = UUID.randomUUID();
        Job job = createTestJob(jobId, "Test Job", ScheduleType.INTERVAL, 300);

        when(jobRepository.findJobsReadyToRun(eq(JobStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(job));
        when(jobScheduleService.processJob(jobId)).thenReturn(true);
        doThrow(new RuntimeException("Kafka error")).when(jobScheduleService).sendKafkaEvent(job);

        // When
        jobScheduler.processScheduledJobs();

        // Then
        verify(jobScheduleService, times(1)).processJob(jobId);
        verify(jobScheduleService, times(1)).sendKafkaEvent(job);
        verify(jobScheduleService, never()).completeJob(any());
        verify(jobScheduleService, times(1)).markJobFailed(jobId);
    }

    @Test
    @DisplayName("성공: 하나의 Job 실패해도 다른 Job은 정상 처리")
    void processScheduledJobs_independentTransactions() {
        // Given
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();

        Job job1 = createTestJob(jobId1, "Job 1", ScheduleType.INTERVAL, 300);
        Job job2 = createTestJob(jobId2, "Job 2", ScheduleType.INTERVAL, 600);

        when(jobRepository.findJobsReadyToRun(eq(JobStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(job1, job2));
        when(jobScheduleService.processJob(jobId1)).thenReturn(true);
        when(jobScheduleService.processJob(jobId2)).thenReturn(true);
        doThrow(new RuntimeException("Kafka error")).when(jobScheduleService).sendKafkaEvent(job1);

        // When
        jobScheduler.processScheduledJobs();

        // Then
        // Job1은 실패 처리
        verify(jobScheduleService, times(1)).markJobFailed(jobId1);

        // Job2는 정상 처리
        verify(jobScheduleService, times(1)).sendKafkaEvent(job2);
        verify(jobScheduleService, times(1)).completeJob(jobId2);
    }
}
