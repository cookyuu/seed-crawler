package com.seed_crawler.core.scheduler;

import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.entity.enums.JobStatus;
import com.seed_crawler.core.repository.JobRepository;
import com.seed_crawler.core.service.JobEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final JobRepository jobRepository;
    private final JobEventProducer jobEventProducer;

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void processScheduledJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<Job> jobsToRun = jobRepository.findJobsReadyToRun(JobStatus.SCHEDULED, now);

        if (jobsToRun.isEmpty()) {
            return;
        }

        log.info("Found {} jobs ready to run", jobsToRun.size());

        for (Job job : jobsToRun) {
            try {
                job.markAsRunning();
                jobRepository.save(job);

                jobEventProducer.sendCrawlRequest(job);

                // 다음 실행 시간 계산 및 상태 업데이트
                job.completeRun();
                jobRepository.save(job);

                log.info("Sent crawl request for jobId={}, title={}, nextRunAt={}",
                        job.getId(), job.getTitle(), job.getNextRunAt());
            } catch (Exception e) {
                log.error("Failed to process job: jobId={}", job.getId(), e);
            }
        }
    }
}
