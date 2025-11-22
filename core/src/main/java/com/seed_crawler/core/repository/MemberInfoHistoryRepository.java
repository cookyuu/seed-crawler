package com.seed_crawler.core.repository;

import com.seed_crawler.core.entity.MemberInfoHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberInfoHistoryRepository extends JpaRepository<MemberInfoHistory, UUID> {
}
