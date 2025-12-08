package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.AuthDto;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.entity.MemberActiveHistory;
import com.seed_crawler.core.global.auth.JwtTokenProvider;
import com.seed_crawler.core.global.context.UserContextManager;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.log.LogEvent;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.repository.MemberActiveHistoryRepository;
import com.seed_crawler.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final MemberRepository memberRepository;
    private final MemberActiveHistoryRepository memberActiveHistoryRepository;
    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserContextManager userContextManager;

    @Override
    @LogEvent("login_attempt")
    @Transactional
    public AuthDto.LoginResult login(String loginId, String password, String ipAddress, String userAgent) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));
        if (member.isAccountLock()) {
            memberActiveHistoryRepository.save(MemberActiveHistory.createLoginHistory(member, ipAddress, userAgent, false));
            throw new AppException(ErrorCode.AUTH_LOCKED, "사용자 계정이 잠김 상태입니다.", "auth.login.error", null);
        }
        if (!passwordEncoder.matches(password, member.getPassword())) {
            member.increasePasswordFailCount();
            memberActiveHistoryRepository.save(MemberActiveHistory.createLoginHistory(member, ipAddress, userAgent, false));
            throw new AppException(ErrorCode.INVALID_PASSWORD, "비밀번호가 일치하지 않습니다.", "auth.login.error", null);
        }
        member.resetPasswordFailCount();
        AuthDto.TokenResponse tokenResponse = jwtTokenProvider.generateTokens(member.getId(), member.getRole());
        tokenService.storeRefreshToken(member.getId(), tokenResponse.getRefreshToken(), jwtTokenProvider.getRefreshTokenExpirationMs());
        userContextManager.setUserId(member.getId());

        memberActiveHistoryRepository.save(MemberActiveHistory.createLoginHistory(member, ipAddress, userAgent, true));

        return new AuthDto.LoginResult(member.getId(), member.getLoginId(), tokenResponse);
    }

    @Override
    @LogEvent("logout_attempt")
    @Transactional
    public void logout(UUID memberId, String accessToken, String ipAddress, String userAgent) {
        userContextManager.setUserId(memberId);

        // 1. DB 작업 먼저 수행 (트랜잭션 내)
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member != null) {
            memberActiveHistoryRepository.save(MemberActiveHistory.createLogoutHistory(member, ipAddress, userAgent));
        }

        // 2. Redis 작업은 DB 커밋 성공 후 수행 (실패해도 로그아웃 기록은 보존)
        // 주의: 이 시점에서 트랜잭션이 아직 커밋되지 않았으므로,
        // Redis 실패 시 전체 롤백됨. 하지만 Redis 실패는 드물고,
        // 로그아웃 기록보다 토큰 무효화가 더 중요하므로 이 순서가 적절함.
        tokenService.invalidateRefreshToken(memberId);

        long remainingMs = jwtTokenProvider.getRemainingExpirationMs(accessToken);
        if (remainingMs > 0) {
            tokenService.addToBlacklist(accessToken, remainingMs);
        }
    }
}
