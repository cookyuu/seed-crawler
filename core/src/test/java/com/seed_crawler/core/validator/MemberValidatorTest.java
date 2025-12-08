package com.seed_crawler.core.validator;

import com.seed_crawler.core.global.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberValidatorTest {

    MemberValidator v = new MemberValidator();

    @Test
    @DisplayName("loginId 성공")
    void loginId_valid() {
        v.validateLoginId("testUser_123");
    }

    @Test
    @DisplayName("loginId 실패 - admin 금지")
    void loginId_invalid_admin() {
        assertThatThrownBy(() -> v.validateLoginId("admin123"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("이메일 실패 - 임시 도메인")
    void email_invalid_tempDomain() {
        assertThatThrownBy(() -> v.validateEmail("user@spam.com"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("비밀번호 실패 - 패턴 불일치")
    void password_invalid_pattern() {
        assertThatThrownBy(() -> v.validatePassword("aaaa1111"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("비밀번호 실패 - 금지 문자열")
    void password_invalid_common() {
        assertThatThrownBy(() -> v.validatePassword("Qwerty1!"))
                .isInstanceOf(AppException.class);
    }
}