package com.seed_crawler.core.service;

import com.seed_crawler.core.global.auth.JwtTokenProvider;
import com.seed_crawler.core.global.context.UserContextManager;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.log.LogEvent;
import com.seed_crawler.core.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final StringRedisTemplate redisTemplate;
    private final UserContextManager userContextManager;
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Override
    @LogEvent("store_refresh_token")
    public void storeRefreshToken(UUID memberId, String refreshToken, long ttlMillis) {
        userContextManager.setUserId(memberId);
        redisTemplate.opsForValue()
                .set(REFRESH_PREFIX + memberId.toString(), refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    @LogEvent("invalidate_refresh_token")
    public void invalidateRefreshToken(UUID memberId) {
        userContextManager.setUserId(memberId);
        redisTemplate.delete(REFRESH_PREFIX + memberId.toString());
    }

    @Override
    @LogEvent("validate_refresh_token")
    public UUID validateRefreshToken(String refreshToken, JwtTokenProvider jwtProvider) {
        UUID memberId = jwtProvider.getMemberId(refreshToken);
        userContextManager.setUserId(memberId);
        String storedToken = redisTemplate.opsForValue().get(REFRESH_PREFIX + memberId.toString());

        if (storedToken == null || !storedToken.equals(refreshToken))
            throw new AppException(ErrorCode.INVALID_TOKEN, "유효하지 않거나 만료된 Refresh Token 입니다.", "auth.refresh.error", null);

        return memberId;
    }

    @Override
    @LogEvent("add_to_blacklist")
    public void addToBlacklist(String accessToken, long ttlMillis) {
        redisTemplate.opsForValue()
                .set(BLACKLIST_PREFIX + accessToken, "logout", ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + accessToken));
    }
}
