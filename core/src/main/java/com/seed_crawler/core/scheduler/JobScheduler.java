package com.seed_crawler.core.scheduler;

import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.entity.enums.JobStatus;
import com.seed_crawler.core.repository.JobRepository;
import com.seed_crawler.core.service.JobScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final JobRepository jobRepository;
    private final JobScheduleService jobScheduleService;

    @Scheduled(fixedRate = 10000)
    public void processScheduledJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<Job> jobsToRun = findJobsToRun(now);

        if (jobsToRun.isEmpty()) {
            return;
        }

        log.info("Found {} jobs ready to run", jobsToRun.size());

        for (Job job : jobsToRun) {
            processJobWithIsolatedTransaction(job);
        }
    }

    @Transactional(readOnly = true)
    public List<Job> findJobsToRun(LocalDateTime now) {
        return jobRepository.findJobsReadyToRun(JobStatus.SCHEDULED, now);
    }

    private void processJobWithIsolatedTransaction(Job job) {
        UUID jobId = job.getId();

        try {
            // 1단계: DB 상태를 RUNNING으로 변경 (독립 트랜잭션)
            boolean processed = jobScheduleService.processJob(jobId);
            if (!processed) {
                return;
            }

            // 2단계: Kafka 이벤트 발행 (트랜잭션 외부)
            jobScheduleService.sendKafkaEvent(job);

            // 3단계: 다음 실행 시간 설정 (독립 트랜잭션)
            jobScheduleService.completeJob(jobId);

            log.info("Successfully processed job: jobId={}, title={}", jobId, job.getTitle());

        } catch (Exception e) {
            log.error("Failed to process job: jobId={}", jobId, e);
            // 실패 시 상태를 STOP으로 변경 (독립 트랜잭션)
            try {
                jobScheduleService.markJobFailed(jobId);
            } catch (Exception ex) {
                log.error("Failed to mark job as failed: jobId={}", jobId, ex);
            }
        }
    }
}
