package com.seed_crawler.core.repository;

import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    @Query("SELECT j FROM Job j WHERE j.enabled = true AND j.status = :status AND j.nextRunAt <= :now")
    List<Job> findJobsReadyToRun(@Param("status") JobStatus status, @Param("now") LocalDateTime now);
}
