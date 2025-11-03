package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.Signup;

public interface MemberService {
    Signup.Result signup(String loginId, String password, String nickname, String email);
}
