package com.seed_crawler.core.repository;

import com.seed_crawler.core.entity.JobInfoHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobInfoHistoryRepository extends JpaRepository<JobInfoHistory, UUID> {
}
