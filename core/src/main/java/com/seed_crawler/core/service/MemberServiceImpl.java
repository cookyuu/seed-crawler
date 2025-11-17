package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.Signup;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.log.LogEvent;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.repository.MemberRepository;
import com.seed_crawler.core.validator.MemberValidator;
import com.sun.jdi.request.DuplicateRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final MemberValidator memberValidator;

    private final PasswordEncoder passwordEncoder;

    @Override
    @LogEvent("signup_attempt")
    public Signup.Result signup(String loginId, String password, String nickname, String email) {
        memberValidator.validateLoginId(loginId);
        memberValidator.validateEmail(email);
        memberValidator.validatePassword(password);

        if (memberRepository.existsByLoginId(loginId)) {
            throw new AppException(ErrorCode.DUPLICATE_REQUEST_EXCEPTION, "이미 등록된 로그인 아이디입니다.","member.signup.error",null);
        }
        if (memberRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.DUPLICATE_REQUEST_EXCEPTION, "이미 등록된 이메일입니다.","member.signup.error",null);
        }
        String encodedPw = passwordEncoder.encode(password);
        Member member = memberRepository.save(
                Member.builder().loginId(loginId).password(encodedPw).nickname(nickname).email(email).build()
        );
        MDC.put("userId", member.getId().toString());
        return new Signup.Result(member.getId(), member.getLoginId(), member.getNickname(), member.getEmail());
    }
}