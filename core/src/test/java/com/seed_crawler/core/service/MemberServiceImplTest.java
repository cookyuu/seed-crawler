package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.MemberDto;
import com.seed_crawler.core.dto.command.MemberSignupCommand;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.global.context.UserContextManager;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.repository.MemberRepository;
import com.seed_crawler.core.validator.MemberValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberServiceImplTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    MemberValidator validator;

    @Mock
    PasswordEncoder encoder;

    @Mock
    UserContextManager userContextManager;

    @InjectMocks
    MemberServiceImpl service;

    @Captor
    ArgumentCaptor<com.seed_crawler.core.entity.Member> memberCaptor;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("회원가입 성공 - 저장되는 Member 값 검증")
    void signup_success() {
        // Given
        UUID id = UUID.randomUUID();
        String loginId = "testUser";
        String password = "Aa1!aaaa";
        String email = "test@example.com";
        String nickname = "닉";

        MemberSignupCommand command = MemberSignupCommand.builder()
                .loginId(loginId)
                .password(password)
                .nickname(nickname)
                .email(email)
                .build();

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

        // When
        MemberDto.Result result = service.signup(command);

        // Then
        verify(validator).validateLoginId(loginId);
        verify(validator).validatePassword(password);
        verify(validator).validateEmail(email);
        verify(encoder).encode(password);
        verify(userContextManager).setUserId(any());

        verify(memberRepository).save(memberCaptor.capture());
        Member captured = memberCaptor.getValue();

        assertThat(captured.getLoginId()).isEqualTo(loginId);
        assertThat(captured.getPassword()).isEqualTo("ENCODED");
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 아이디")
    void signup_duplicate_loginId() {
        // Given
        MemberSignupCommand command = MemberSignupCommand.builder()
                .loginId("testUser")
                .password("Aa1!aaaa")
                .nickname("닉")
                .email("test@example.com")
                .build();

        when(memberRepository.existsByLoginId("testUser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.signup(command))
                .isInstanceOf(AppException.class)
                .hasMessage("이미 등록된 로그인 아이디입니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일")
    void signup_duplicate_email() {
        // Given
        MemberSignupCommand command = MemberSignupCommand.builder()
                .loginId("testUser")
                .password("Aa1!aaaa")
                .nickname("닉")
                .email("test@example.com")
                .build();

        when(memberRepository.existsByLoginId("testUser")).thenReturn(false);
        when(memberRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.signup(command))
                .isInstanceOf(AppException.class)
                .hasMessage("이미 등록된 이메일입니다.");
    }
}