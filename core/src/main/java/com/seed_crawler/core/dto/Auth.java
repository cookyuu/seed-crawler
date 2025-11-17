package com.seed_crawler.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class Auth {

    @Getter
    @AllArgsConstructor
    public static class LoginRequest {
        private String loginId;
        private String password;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginResult {
        private UUID memberId;
        private String loginId;
        private TokenResponse token;
    }
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginResponse {
        private String loginId;
        private String accessToken;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
    }
}
