package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.MemberDto;

public interface MemberService {
    MemberDto.Result signup(String loginId, String password, String nickname, String email);
}
