package com.seed_crawler.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seed_crawler.core.dto.AuthDto;
import com.seed_crawler.core.entity.enums.MemberRole;
import com.seed_crawler.core.global.auth.CustomUserDetails;
import com.seed_crawler.core.global.auth.JwtTokenProvider;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private CustomUserDetails mockUser;
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockUser = new CustomUserDetails(memberId, MemberRole.USER, false, true);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("성공: 로그인 성공 → JSON 응답 구조 검증 및 쿠키 설정 확인")
    void login_success() throws Exception {
        // Given
        String loginId = "testUser";
        String password = "password123";
        String accessToken = "accessToken123";
        String refreshToken = "refreshToken123";

        AuthDto.TokenResponse tokenResponse = new AuthDto.TokenResponse(accessToken, refreshToken);
        AuthDto.LoginResult result = new AuthDto.LoginResult(memberId, loginId, tokenResponse);

        when(authService.login(loginId, password)).thenReturn(result);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(1209600000L); // 14 days

        AuthDto.LoginRequest req = new AuthDto.LoginRequest(loginId, password);

        // When & Then
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value(loginId))
                .andExpect(jsonPath("$.data.accessToken").value(accessToken))
                .andExpect(jsonPath("$.message").value("로그인이 완료되었습니다."))
                .andExpect(jsonPath("$.messageCode").value("auth.login.success"))
                .andExpect(jsonPath("$.error").isEmpty())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("실패: 로그인 실패 - 비밀번호 불일치 → 에러 JSON 구조 검증")
    void login_wrongPassword_fail() throws Exception {
        // Given
        String loginId = "testUser";
        String password = "wrongPassword";

        AppException exception = new AppException(
                ErrorCode.INVALID_PASSWORD,
                "비밀번호가 일치하지 않습니다.",
                "auth.login.error",
                null
        );

        when(authService.login(loginId, password)).thenThrow(exception);

        AuthDto.LoginRequest req = new AuthDto.LoginRequest(loginId, password);

        // When & Then
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.error.message").value("비밀번호가 일치하지 않습니다."))
                .andExpect(jsonPath("$.error.messageCode").value("auth.login.error"));
    }

    @Test
    @DisplayName("실패: 로그인 실패 - 계정 잠김 → 에러 JSON 구조 검증")
    void login_accountLocked_fail() throws Exception {
        // Given
        String loginId = "testUser";
        String password = "password123";

        AppException exception = new AppException(
                ErrorCode.AUTH_LOCKED,
                "사용자 계정이 잠김 상태입니다.",
                "auth.login.error",
                null
        );

        when(authService.login(loginId, password)).thenThrow(exception);

        AuthDto.LoginRequest req = new AuthDto.LoginRequest(loginId, password);

        // When & Then
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_LOCK"))
                .andExpect(jsonPath("$.error.message").value("사용자 계정이 잠김 상태입니다."))
                .andExpect(jsonPath("$.error.messageCode").value("auth.login.error"));
    }

    @Test
    @DisplayName("성공: 로그아웃 성공 → JSON 응답 구조 검증 및 쿠키 삭제 확인")
    void logout_success() throws Exception {
        // Given
        String accessToken = "Bearer accessToken123";

        when(jwtTokenProvider.resolveToken(org.mockito.ArgumentMatchers.any())).thenReturn("accessToken123");

        // When & Then
        mvc.perform(post("/api/auth/logout")
                        .header("Authorization", accessToken)
                        .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."))
                .andExpect(jsonPath("$.messageCode").value("auth.logout.success"))
                .andExpect(jsonPath("$.error").isEmpty())
                .andExpect(header().exists("Set-Cookie"));

        verify(authService).logout(eq(memberId), anyString());
    }

    @Test
    @DisplayName("실패: 로그아웃 실패 - 인증되지 않은 사용자")
    void logout_unauthorized_fail() throws Exception {
        // When & Then
        mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }
}
