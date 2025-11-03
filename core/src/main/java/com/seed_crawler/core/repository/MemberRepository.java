package com.seed_crawler.core.repository;

import com.seed_crawler.core.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRespository extends JpaRepository<Member, String> {
}
