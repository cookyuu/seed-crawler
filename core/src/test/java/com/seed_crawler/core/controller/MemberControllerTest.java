package com.seed_crawler.core.controller;

import com.seed_crawler.core.dto.MemberDto;
import com.seed_crawler.core.dto.command.MemberSignupCommand;
import com.seed_crawler.core.dto.command.MemberUpdateCommand;
import com.seed_crawler.core.dto.command.MemberWithdrawCommand;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.global.context.UserContextManager;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.repository.MemberRepository;
import com.seed_crawler.core.service.MemberServiceImpl;
import com.seed_crawler.core.validator.MemberValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberControllerTest {

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

    @Test
    @DisplayName("회원정보 수정 성공 - 닉네임, 이메일 변경")
    void updateMember_success() {
        // Given
        UUID memberId = UUID.randomUUID();
        String newNickname = "새닉네임";
        String newEmail = "new@example.com";

        Member existingMember = Member.builder()
                .id(memberId)
                .loginId("testUser")
                .password("ENCODED")
                .nickname("기존닉네임")
                .email("old@example.com")
                .build();

        MemberUpdateCommand command = MemberUpdateCommand.builder()
                .memberId(memberId)
                .nickname(newNickname)
                .email(newEmail)
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(existingMember));
        when(memberRepository.existsByEmail(newEmail)).thenReturn(false);

        // When
        MemberDto.UpdateResult result = service.updateMember(command);

        // Then
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getNickname()).isEqualTo(newNickname);
        assertThat(result.getEmail()).isEqualTo(newEmail);
    }

    @Test
    @DisplayName("회원정보 수정 성공 - 비밀번호 변경")
    void updateMember_password_success() {
        // Given
        UUID memberId = UUID.randomUUID();
        String currentPassword = "OldPass1!";
        String newPassword = "NewPass1!";

        Member existingMember = Member.builder()
                .id(memberId)
                .loginId("testUser")
                .password("ENCODED_OLD")
                .nickname("닉네임")
                .email("test@example.com")
                .build();

        MemberUpdateCommand command = MemberUpdateCommand.builder()
                .memberId(memberId)
                .nickname("닉네임")
                .email("test@example.com")
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(existingMember));
        when(encoder.matches(currentPassword, "ENCODED_OLD")).thenReturn(true);
        when(encoder.encode(newPassword)).thenReturn("ENCODED_NEW");

        // When
        MemberDto.UpdateResult result = service.updateMember(command);

        // Then
        verify(validator).validatePassword(newPassword);
        verify(encoder).encode(newPassword);
        assertThat(result.getMemberId()).isEqualTo(memberId);
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 현재 비밀번호 불일치")
    void updateMember_wrong_password() {
        // Given
        UUID memberId = UUID.randomUUID();

        Member existingMember = Member.builder()
                .id(memberId)
                .loginId("testUser")
                .password("ENCODED_OLD")
                .nickname("닉네임")
                .email("test@example.com")
                .build();

        MemberUpdateCommand command = MemberUpdateCommand.builder()
                .memberId(memberId)
                .currentPassword("wrongPassword")
                .newPassword("NewPass1!")
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(existingMember));
        when(encoder.matches("wrongPassword", "ENCODED_OLD")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.updateMember(command))
                .isInstanceOf(AppException.class)
                .hasMessage("현재 비밀번호가 일치하지 않습니다");
    }

    @Test
    @DisplayName("회원탈퇴 성공")
    void withdraw_success() {
        // Given
        UUID memberId = UUID.randomUUID();
        String password = "Pass1!";

        Member existingMember = Member.builder()
                .id(memberId)
                .loginId("testUser")
                .password("ENCODED")
                .nickname("닉네임")
                .email("test@example.com")
                .build();

        MemberWithdrawCommand command = MemberWithdrawCommand.builder()
                .memberId(memberId)
                .password(password)
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(existingMember));
        when(encoder.matches(password, "ENCODED")).thenReturn(true);

        // When
        MemberDto.WithdrawResult result = service.withdraw(command);

        // Then
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(existingMember.isActive()).isFalse();
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 비밀번호 불일치")
    void withdraw_wrong_password() {
        // Given
        UUID memberId = UUID.randomUUID();

        Member existingMember = Member.builder()
                .id(memberId)
                .loginId("testUser")
                .password("ENCODED")
                .nickname("닉네임")
                .email("test@example.com")
                .build();

        MemberWithdrawCommand command = MemberWithdrawCommand.builder()
                .memberId(memberId)
                .password("wrongPassword")
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(existingMember));
        when(encoder.matches("wrongPassword", "ENCODED")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.withdraw(command))
                .isInstanceOf(AppException.class)
                .hasMessage("비밀번호가 일치하지 않습니다");
    }
}