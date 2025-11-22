package com.seed_crawler.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;


public class MemberDto {
    @Getter
    @AllArgsConstructor
    public static class SignupRequest {
        private String loginId;
        private String password;
        private String nickname;
        private String email;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SignupResponse {
        private UUID memberId;
        private String loginId;
        private String nickname;
        private String email;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Result {
        private UUID memberId;
        private String loginId;
        private String nickname;
        private String email;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateRequest {
        private String nickname;
        private String email;
        private String currentPassword;
        private String newPassword;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateResult {
        private UUID memberId;
        private String nickname;
        private String email;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateResponse {
        private UUID memberId;
        private String nickname;
        private String email;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WithdrawRequest {
        private String password;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WithdrawResult {
        private UUID memberId;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WithdrawResponse {
        private UUID memberId;
    }
}
