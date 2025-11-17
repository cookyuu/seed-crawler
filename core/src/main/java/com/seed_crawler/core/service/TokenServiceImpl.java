package com.seed_crawler.core.service;

import com.seed_crawler.core.global.auth.JwtTokenProvider;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenSerivce {
    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "refresh:";

    public void storeRefreshToken(UUID memberId, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue()
                .set(PREFIX + memberId.toString(), refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public void invalidateRefreshToken(UUID memberId) {
        redisTemplate.delete(PREFIX + memberId.toString());
    }

    public UUID validateRefreshToken(String refreshToken, JwtTokenProvider jwtProvider) {
        UUID memberId = jwtProvider.getMemberId(refreshToken);
        String storedToken = redisTemplate.opsForValue().get(PREFIX + memberId.toString());

        if (storedToken == null || !storedToken.equals(refreshToken))
            throw new AppException(ErrorCode.INVALID_TOKEN, "유효하지 않거나 만료된 Refresh Token 입니다.", "auth.refresh.error", null);

        return memberId;
    }
}
