package com.seed_crawler.core.validator;

import com.seed_crawler.core.entity.enums.ScheduleType;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.response.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.*;

class JobValidatorTest {
    private JobValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JobValidator();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("성공: 정상 URL은 예외 없이 통과된다")
    void validateUrl_success() {
        // Given
        String url = "https://google.com";

        // When & Then
        assertThatCode(() -> validator.validateUrl(url))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("실패: 잘못된 URL → VALIDATION_ERROR 발생")
    void validateUrl_fail_invalid() {
        // Given
        String badUrl = "ht!tp:/wrong-url";

        // When
        AppException ex = catchThrowableOfType(
                () -> validator.validateUrl(badUrl),
                AppException.class
        );

        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(ex.getMessage()).contains("URL 주소값 검증 오류");
    }

    @Test
    @DisplayName("성공: 유효한 CRON 표현식은 정상 통과된다")
    void validateSchedule_cron_success() {
        // Given
        ScheduleType type = ScheduleType.CRON;
        String cron = "0/10 * * * * *";

        // When & Then
        assertThatCode(() -> validator.validateSchedule(type, cron))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("실패: 잘못된 CRON 표현식 → VALIDATION_ERROR 발생")
    void validateSchedule_cron_fail_invalid() {
        // Given
        ScheduleType type = ScheduleType.CRON;
        String badCron = "INVALID_CRON";

        // When
        AppException ex = catchThrowableOfType(
                () -> validator.validateSchedule(type, badCron),
                AppException.class
        );

        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(ex.getMessage()).contains("Job Schedule CRON 값 검증 오류");
    }

    @Test
    @DisplayName("성공: INTERVAL 값이 1 이상 정수면 정상 통과된다")
    void validateSchedule_interval_success() {
        // Given
        ScheduleType type = ScheduleType.INTERVAL;
        String interval = "10";

        // When & Then
        assertThatCode(() -> validator.validateSchedule(type, interval))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("실패: INTERVAL 값이 숫자가 아니면 VALIDATION_ERROR 발생")
    void validateSchedule_interval_fail_notNumber() {
        // Given
        ScheduleType type = ScheduleType.INTERVAL;
        String interval = "abc";

        // When
        AppException ex = catchThrowableOfType(
                () -> validator.validateSchedule(type, interval),
                AppException.class
        );

        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(ex.getMessage()).contains("숫자가 아님");
    }

    @Test
    @DisplayName("실패: INTERVAL 값이 0 이하이면 VALIDATION_ERROR 발생")
    void validateSchedule_interval_fail_negative() {
        // Given
        ScheduleType type = ScheduleType.INTERVAL;
        String interval = "-5";

        // When
        AppException ex = catchThrowableOfType(
                () -> validator.validateSchedule(type, interval),
                AppException.class
        );

        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(ex.getMessage()).contains("양수가 아님");
    }
}