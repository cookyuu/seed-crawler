package com.seed_crawler.core.service;

import com.seed_crawler.core.global.auth.JwtTokenProvider;

import java.util.UUID;

public interface TokenService {
    void storeRefreshToken(UUID memberId, String refreshToken, long ttlMillis);
    void invalidateRefreshToken(UUID memberId);
    UUID validateRefreshToken(String refreshToken, JwtTokenProvider jwtProvider);
    void addToBlacklist(String accessToken, long ttlMillis);
    boolean isBlacklisted(String accessToken);
}
