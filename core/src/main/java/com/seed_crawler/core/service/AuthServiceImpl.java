package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.AuthDto;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.global.auth.JwtTokenProvider;
import com.seed_crawler.core.global.context.UserContextManager;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.log.LogEvent;
import com.seed_crawler.core.global.response.ErrorCode;
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
    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserContextManager userContextManager;

    @Override
    @LogEvent("login_attempt")
    @Transactional
    public AuthDto.LoginResult login(String loginId, String password) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));
        if (member.isAccountLock()) {
            throw new AppException(ErrorCode.AUTH_LOCKED, "사용자 계정이 잠김 상태입니다.", "auth.login.error", null);
        }
        if (!passwordEncoder.matches(password, member.getPassword())) {
            member.increasePasswordFailCount();
            throw new AppException(ErrorCode.INVALID_PASSWORD, "비밀번호가 일치하지 않습니다.", "auth.login.error", null);
        }
        member.resetPasswordFailCount();
        AuthDto.TokenResponse tokenResponse = jwtTokenProvider.generateTokens(member.getId(), member.getRole());
        tokenService.storeRefreshToken(member.getId(), tokenResponse.getRefreshToken(), jwtTokenProvider.getRefreshTokenExpirationMs());
        userContextManager.setUserId(member.getId());

        return new AuthDto.LoginResult(member.getId(), member.getLoginId(), tokenResponse);
    }

    @Override
    @LogEvent("logout_attempt")
    public void logout(UUID memberId, String accessToken) {
        userContextManager.setUserId(memberId);

        tokenService.invalidateRefreshToken(memberId);

        long remainingMs = jwtTokenProvider.getRemainingExpirationMs(accessToken);
        if (remainingMs > 0) {
            tokenService.addToBlacklist(accessToken, remainingMs);
        }
    }
}
