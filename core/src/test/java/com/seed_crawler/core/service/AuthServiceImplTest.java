package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.AuthDto;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.entity.MemberActiveHistory;
import com.seed_crawler.core.entity.enums.MemberRole;
import com.seed_crawler.core.global.auth.JwtTokenProvider;
import com.seed_crawler.core.global.context.UserContextManager;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.repository.MemberActiveHistoryRepository;
import com.seed_crawler.core.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    MemberActiveHistoryRepository memberActiveHistoryRepository;

    @Mock
    TokenService tokenService;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    UserContextManager userContextManager;

    @InjectMocks
    AuthServiceImpl authService;

    private static final String TEST_IP = "127.0.0.1";
    private static final String TEST_USER_AGENT = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // Given
        UUID memberId = UUID.randomUUID();
        String loginId = "testUser";
        String password = "password123";
        String encodedPassword = "encodedPassword";

        Member member = Member.builder()
                .id(memberId)
                .loginId(loginId)
                .password(encodedPassword)
                .nickname("닉네임")
                .email("test@example.com")
                .build();

        AuthDto.TokenResponse tokenResponse = new AuthDto.TokenResponse("accessToken", "refreshToken");

        when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(jwtTokenProvider.generateTokens(memberId, MemberRole.USER)).thenReturn(tokenResponse);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(1209600000L);

        // When
        AuthDto.LoginResult result = authService.login(loginId, password, TEST_IP, TEST_USER_AGENT);

        // Then
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getLoginId()).isEqualTo(loginId);
        assertThat(result.getToken().getAccessToken()).isEqualTo("accessToken");
        verify(tokenService).storeRefreshToken(eq(memberId), eq("refreshToken"), anyLong());
        verify(memberActiveHistoryRepository).save(any(MemberActiveHistory.class));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_wrongPassword_fail() {
        // Given
        String loginId = "testUser";
        String password = "wrongPassword";

        Member member = Member.builder()
                .id(UUID.randomUUID())
                .loginId(loginId)
                .password("encodedPassword")
                .nickname("닉네임")
                .email("test@example.com")
                .build();

        when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginId, password, TEST_IP, TEST_USER_AGENT))
                .isInstanceOf(AppException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
        verify(memberActiveHistoryRepository).save(any(MemberActiveHistory.class));
    }

    @Test
    @DisplayName("로그인 실패 - 계정 잠김")
    void login_accountLocked_fail() {
        // Given
        String loginId = "testUser";
        String password = "password";

        Member member = Member.builder()
                .id(UUID.randomUUID())
                .loginId(loginId)
                .password("encodedPassword")
                .nickname("닉네임")
                .email("test@example.com")
                .build();

        // 계정 잠금 상태로 만들기 (5번 실패)
        for (int i = 0; i < 5; i++) {
            member.increasePasswordFailCount();
        }

        when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(member));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginId, password, TEST_IP, TEST_USER_AGENT))
                .isInstanceOf(AppException.class)
                .hasMessage("사용자 계정이 잠김 상태입니다.");
        verify(memberActiveHistoryRepository).save(any(MemberActiveHistory.class));
    }

    @Test
    @DisplayName("로그아웃 성공 - Refresh Token 삭제 및 Access Token 블랙리스트 추가")
    void logout_success() {
        // Given
        UUID memberId = UUID.randomUUID();
        String accessToken = "validAccessToken";
        long remainingMs = 600000L; // 10분

        Member member = Member.builder()
                .id(memberId)
                .loginId("testUser")
                .password("encodedPassword")
                .nickname("닉네임")
                .email("test@example.com")
                .build();

        when(jwtTokenProvider.getRemainingExpirationMs(accessToken)).thenReturn(remainingMs);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // When
        authService.logout(memberId, accessToken, TEST_IP, TEST_USER_AGENT);

        // Then
        verify(userContextManager).setUserId(memberId);
        verify(tokenService).invalidateRefreshToken(memberId);
        verify(tokenService).addToBlacklist(accessToken, remainingMs);
        verify(memberActiveHistoryRepository).save(any(MemberActiveHistory.class));
    }

    @Test
    @DisplayName("로그아웃 성공 - 만료된 토큰은 블랙리스트에 추가하지 않음")
    void logout_expiredToken_noBlacklist() {
        // Given
        UUID memberId = UUID.randomUUID();
        String accessToken = "expiredAccessToken";

        Member member = Member.builder()
                .id(memberId)
                .loginId("testUser")
                .password("encodedPassword")
                .nickname("닉네임")
                .email("test@example.com")
                .build();

        when(jwtTokenProvider.getRemainingExpirationMs(accessToken)).thenReturn(0L);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // When
        authService.logout(memberId, accessToken, TEST_IP, TEST_USER_AGENT);

        // Then
        verify(userContextManager).setUserId(memberId);
        verify(tokenService).invalidateRefreshToken(memberId);
        verify(tokenService, never()).addToBlacklist(anyString(), anyLong());
        verify(memberActiveHistoryRepository).save(any(MemberActiveHistory.class));
    }
}
