package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.Auth;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.global.auth.JwtTokenProvider;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.log.LogEvent;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final MemberRepository memberRepository;
    private final TokenSerivce tokenSerivce;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @LogEvent("login_attempt")
    public Auth.LoginResult login(String loginId, String password) {
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
        Auth.TokenResponse tokenResponse = jwtTokenProvider.generateTokens(member.getId(), member.getRole());
        tokenSerivce.storeRefreshToken(member.getId(), tokenResponse.getRefreshToken(), jwtTokenProvider.getRefreshTokenExpirationMs());
        MDC.put("userId", member.getId().toString());

        return new Auth.LoginResult(member.getId(), member.getLoginId(), tokenResponse);
    }

}
