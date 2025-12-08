package com.seed_crawler.core.converter;

import com.seed_crawler.core.entity.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

/*
 * Schedule 타입에 따라 문자열을 CRON 표현식 또는 Interval(초) 값으로 변환하는 Converter
 */
@Component
public class ScheduleConverter {


    public ParsedSchedule convert(ScheduleType scheduleType, String schedule) {
        if (scheduleType == ScheduleType.CRON) {
            return new ParsedSchedule(schedule, null);
        } else if (scheduleType == ScheduleType.INTERVAL) {
            return new ParsedSchedule(null, Integer.valueOf(schedule));
        }
        return new ParsedSchedule(null, null);
    }

    @Getter
    @AllArgsConstructor
    public static class ParsedSchedule {
        private final String cronExpression;
        private final Integer intervalSec;
    }
}
