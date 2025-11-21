package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.MemberDto;
import com.seed_crawler.core.dto.command.MemberSignupCommand;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.global.context.UserContextManager;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.log.LogEvent;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.repository.MemberRepository;
import com.seed_crawler.core.validator.MemberValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final MemberValidator memberValidator;
    private final PasswordEncoder passwordEncoder;
    private final UserContextManager userContextManager;

    @Override
    @LogEvent("signup_attempt")
    @Transactional
    public MemberDto.Result signup(MemberSignupCommand command) {
        memberValidator.validateLoginId(command.getLoginId());
        memberValidator.validateEmail(command.getEmail());
        memberValidator.validatePassword(command.getPassword());

        if (memberRepository.existsByLoginId(command.getLoginId())) {
            throw new AppException(ErrorCode.DUPLICATE_REQUEST_EXCEPTION, "이미 등록된 로그인 아이디입니다.","member.signup.error",null);
        }
        if (memberRepository.existsByEmail(command.getEmail())) {
            throw new AppException(ErrorCode.DUPLICATE_REQUEST_EXCEPTION, "이미 등록된 이메일입니다.","member.signup.error",null);
        }
        String encodedPw = passwordEncoder.encode(command.getPassword());
        Member member = memberRepository.save(
                Member.builder()
                        .loginId(command.getLoginId())
                        .password(encodedPw)
                        .nickname(command.getNickname())
                        .email(command.getEmail())
                        .build()
        );
        userContextManager.setUserId(member.getId());
        return new MemberDto.Result(member.getId(), member.getLoginId(), member.getNickname(), member.getEmail());
    }
}