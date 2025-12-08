package com.seed_crawler.core.controller;

import com.seed_crawler.core.dto.AuthDto;
import com.seed_crawler.core.global.auth.CustomUserDetails;
import com.seed_crawler.core.global.auth.JwtTokenProvider;
import com.seed_crawler.core.global.response.ApiResponse;
import com.seed_crawler.core.global.response.ApiResponseFactory;
import com.seed_crawler.core.service.AuthService;
import com.seed_crawler.core.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils cookieUtils;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(@RequestBody AuthDto.LoginRequest req, HttpServletRequest httpReq, HttpServletResponse httpRes) {
        long refreshTtlMillis = jwtTokenProvider.getRefreshTokenExpirationMs();
        String ipAddress = getClientIp(httpReq);
        String userAgent = httpReq.getHeader("User-Agent");

        AuthDto.LoginResult result = authService.login(req.getLoginId(), req.getPassword(), ipAddress, userAgent);
        cookieUtils.setCookieHttpOnly(httpRes, "refreshToken", result.getToken().getRefreshToken(), (int)(refreshTtlMillis / 1000));
        AuthDto.LoginResponse payload = new AuthDto.LoginResponse(
                result.getLoginId(),
                result.getToken().getAccessToken());
        var body = ApiResponseFactory.ok(
                payload,
                "로그인이 완료되었습니다.",
                "auth.login.success",
                httpReq
        );
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest httpReq,
            HttpServletResponse httpRes) {

        String accessToken = jwtTokenProvider.resolveToken(httpReq);
        String ipAddress = getClientIp(httpReq);
        String userAgent = httpReq.getHeader("User-Agent");

        authService.logout(user.getMemberId(), accessToken, ipAddress, userAgent);

        // Refresh Token 쿠키 삭제
        cookieUtils.deleteCookie(httpRes, "refreshToken");

        var body = ApiResponseFactory.<Void>ok(
                null,
                "로그아웃이 완료되었습니다.",
                "auth.logout.success",
                httpReq
        );
        return ResponseEntity.ok(body);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
