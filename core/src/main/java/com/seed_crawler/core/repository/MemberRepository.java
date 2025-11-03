package com.seed_crawler.core.repository;

import com.seed_crawler.core.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, String> {
    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);
}
