package com.seed_crawler.core.repository;

import com.seed_crawler.core.entity.MemberActiveHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberActiveHistoryRepository extends JpaRepository<MemberActiveHistory, UUID> {
}
