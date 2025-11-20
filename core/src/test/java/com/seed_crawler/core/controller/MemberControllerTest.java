package com.seed_crawler.core.controller;

import com.seed_crawler.core.dto.MemberDto;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.repository.MemberRepository;
import com.seed_crawler.core.service.MemberServiceImpl;
import com.seed_crawler.core.validator.MemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberDtoServiceImplTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    MemberValidator validator;

    @Mock
    PasswordEncoder encoder;

    @InjectMocks
    MemberServiceImpl service;

    @Captor
    ArgumentCaptor<com.seed_crawler.core.entity.Member> memberCaptor;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("회원가입 성공 - 저장되는 Member 값 검증")
    void signup_success() {
        UUID id = UUID.randomUUID();
        String loginId = "testUser";
        String password = "Aa1!aaaa";
        String email = "test@example.com";
        String nickname = "닉";

        when(memberRepository.existsByLoginId(loginId)).thenReturn(false);
        when(memberRepository.existsByEmail(email)).thenReturn(false);
        when(encoder.encode(password)).thenReturn("ENCODED");
        com.seed_crawler.core.entity.Member saved = com.seed_crawler.core.entity.Member.builder()
                .id(id)
                .loginId(loginId)
                .password("ENCODED")
                .nickname(nickname)
                .email(email)
                .build();

        when(memberRepository.save(any())).thenReturn(saved);

        MemberDto.Result result = service.signup(loginId, password, nickname, email);

        verify(validator).validateLoginId(loginId);
        verify(validator).validatePassword(password);
        verify(validator).validateEmail(email);
        verify(encoder).encode(password);

        verify(memberRepository).save(memberCaptor.capture());
        Member captured = memberCaptor.getValue();

        assertThat(captured.getLoginId()).isEqualTo(loginId);
        assertThat(captured.getPassword()).isEqualTo("ENCODED");
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 아이디")
    void signup_duplicate_loginId() {
        when(memberRepository.existsByLoginId("testUser")).thenReturn(true);

        assertThatThrownBy(() ->
                service.signup("testUser", "Aa1!aaaa", "닉", "test@example.com")
        )
                .isInstanceOf(AppException.class)
                .hasMessage("이미 등록된 로그인 아이디입니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일")
    void signup_duplicate_email() {
        when(memberRepository.existsByLoginId("testUser")).thenReturn(false);
        when(memberRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                service.signup("testUser", "Aa1!aaaa", "닉", "test@example.com")
        )
                .isInstanceOf(AppException.class)
                .hasMessage("이미 등록된 이메일입니다.");
    }
}