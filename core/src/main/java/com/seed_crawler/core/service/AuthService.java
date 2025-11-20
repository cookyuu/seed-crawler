package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.AuthDto;

public interface AuthService {
    AuthDto.LoginResult login(String loginId, String password);
}
