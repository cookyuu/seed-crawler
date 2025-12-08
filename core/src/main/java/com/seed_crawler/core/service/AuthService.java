package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.AuthDto;

import java.util.UUID;

public interface AuthService {
    AuthDto.LoginResult login(String loginId, String password, String ipAddress, String userAgent);
    void logout(UUID memberId, String accessToken, String ipAddress, String userAgent);
}
