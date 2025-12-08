package com.seed_crawler.core.service;

import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobScheduleService {

    private final JobRepository jobRepository;
    private final JobEventProducer jobEventProducer;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElse(null);

        if (job == null) {
            log.warn("Job not found: jobId={}", jobId);
            return false;
        }

        job.markAsRunning();
        jobRepository.saveAndFlush(job);

        return true;
    }

    public void sendKafkaEvent(Job job) {
        jobEventProducer.sendCrawlRequest(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElse(null);

        if (job == null) {
            log.warn("Job not found for completion: jobId={}", jobId);
            return;
        }

        job.completeRun();
        jobRepository.saveAndFlush(job);

        log.info("Completed job scheduling: jobId={}, nextRunAt={}", jobId, job.getNextRunAt());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobFailed(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElse(null);

        if (job == null) {
            return;
        }

        job.stop();
        jobRepository.saveAndFlush(job);

        log.warn("Marked job as failed: jobId={}", jobId);
    }
}
