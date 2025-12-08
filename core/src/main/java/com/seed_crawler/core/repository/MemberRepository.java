package com.seed_crawler.core.repository;

import com.seed_crawler.core.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    Optional<Member> findByLoginId(String loginId);
}
