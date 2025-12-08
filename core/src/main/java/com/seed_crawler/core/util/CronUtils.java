package com.seed_crawler.core.util;

import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;

public class CronUtils {

    public static LocalDateTime getNextExecutionTime(String cronExpression) {
        return getNextExecutionTime(cronExpression, LocalDateTime.now());
    }

    public static LocalDateTime getNextExecutionTime(String cronExpression, LocalDateTime from) {
        CronExpression cron = CronExpression.parse(cronExpression);
        return cron.next(from);
    }

    public static boolean isValidCronExpression(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
