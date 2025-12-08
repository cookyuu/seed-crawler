package com.seed_crawler.core.validator;

import com.seed_crawler.core.entity.enums.ScheduleType;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.response.ErrorCode;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component
public class JobValidator {
    public void validateUrl(String url) {
        try {
            new URL(url).toURI();
        } catch (Exception e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "URL 주소값 검증 오류", "job.validation.error", null);
        }
    }

    public void validateSchedule(ScheduleType scheduleType, String schedule) {
        if (scheduleType.equals(ScheduleType.CRON) && !CronExpression.isValidExpression(schedule)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Job Schedule CRON 값 검증 오류", "job.validation.error", null);
        }
        if (scheduleType.equals(ScheduleType.INTERVAL)) {
            validateInterval(schedule);
        }
    }

    private void validateInterval(String schedule) {
        try {
            int value = Integer.parseInt(schedule);
            if (value <= 0) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Job Schedule INTERVAL 값 검증 오류(양수가 아님)", "job.validation.error", null);
            }
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Job Schedule INTERVAL 값 검증 오류(숫자가 아님)", "job.validation.error", null);
        }
    }
}
