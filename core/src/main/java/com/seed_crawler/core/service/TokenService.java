package com.seed_crawler.core.service;

import com.seed_crawler.core.global.auth.JwtTokenProvider;

import java.util.UUID;

public interface TokenService {
    public void storeRefreshToken(UUID memberId, String refreshToken, long ttlMillis);
    public void invalidateRefreshToken(UUID memberId);
    public UUID validateRefreshToken(String refreshToken, JwtTokenProvider jwtProvider);
}
