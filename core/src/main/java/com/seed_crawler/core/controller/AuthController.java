package com.seed_crawler.core.controller;

import com.seed_crawler.core.dto.Auth;
import com.seed_crawler.core.global.auth.JwtTokenProvider;
import com.seed_crawler.core.global.response.ApiResponse;
import com.seed_crawler.core.global.response.ApiResponseFactory;
import com.seed_crawler.core.service.AuthService;
import com.seed_crawler.core.util.CookieUtils;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Auth.LoginResponse>> login(@RequestBody Auth.LoginRequest req, HttpServletRequest httpReq, HttpServletResponse httpRes) {
        long refreshTtlMillis = jwtTokenProvider.getRefreshTokenExpirationMs();
        Auth.LoginResult result = authService.login(req.getLoginId(), req.getPassword());
        CookieUtils.setCookieHttpOnly(httpRes, "refreshToken", result.getToken().getRefreshToken(), (int)(refreshTtlMillis / 1000));
        Auth.LoginResponse payload = new Auth.LoginResponse(
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
}
