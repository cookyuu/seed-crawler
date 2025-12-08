package com.seed_crawler.core.validator;

import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.response.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class MemberValidator {

    public void validateLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "로그인 ID는 비어 있을 수 없습니다.","member.validate.error", null);
        }

        if (!loginId.matches("^[a-zA-Z0-9_-]{4,20}$")) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "로그인 ID는 4~20자의 영문자, 숫자, 언더바(_) 또는 하이픈(-)만 사용할 수 있습니다.", "member.validate.error", null);
        }

        if (loginId.toLowerCase().contains("admin") || loginId.toLowerCase().contains("root")) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "해당 로그인 ID는 사용할 수 없습니다.", "member.validate.error", null);
        }
    }

    public void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "이메일은 비어 있을 수 없습니다.", "member.validate.error", null);
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "이메일 형식이 올바르지 않습니다.", "member.validate.error", null);
        }

        String lower = email.toLowerCase();
        if (lower.endsWith("@spam.com") || lower.endsWith("@temp-mail.org")) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "임시 이메일 도메인은 사용할 수 없습니다.", "member.validate.error", null);
        }
    }

    public void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "비밀번호는 비어 있을 수 없습니다.", "member.validate.error", null);
        }

        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=?]).{8,64}$";
        if (!password.matches(pattern)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "비밀번호는 8자 이상이며, 대문자/소문자/숫자/특수문자를 모두 포함해야 합니다.", "member.validate.error", null);
        }

        String lower = password.toLowerCase();
        if (lower.contains("password") || lower.contains("qwerty") || lower.contains("1234")) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "보안상 안전하지 않은 비밀번호입니다.", "member.validate.error", null);
        }
    }
}
