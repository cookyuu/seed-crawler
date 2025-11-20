package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.ScheduleType;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.log.LogEvent;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.repository.JobRepository;
import com.seed_crawler.core.repository.MemberRepository;
import com.seed_crawler.core.validator.JobValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final MemberRepository memberRepository;
    private final JobValidator jobValidator;

    @Override
    @LogEvent("job_save")
    @Transactional
    public JobDto.SaveResult saveJob(UUID memberId, String title, String description, String targetUrl, JobExecutionType jobExecutionType, ScheduleType scheduleType,
                                     String schedule, Map<String, Object> headerParameters, Map<String, Object> bodyParameters, Map<String, Object> queryParameters,
                                     Integer retryLimit, Integer retryIntervalSec, Integer timeoutSec, String callbackUrl) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND,"회원 정보를 찾을 수 없습니다","job.save.error",null));

        jobValidator.validateUrl(targetUrl);
        jobValidator.validateSchedule(scheduleType, schedule);
        if (callbackUrl != null) {
            jobValidator.validateUrl(callbackUrl);
        }
        String cronExpression = scheduleType.equals(ScheduleType.CRON) ? schedule : null;
        Integer intervalSec = scheduleType.equals(ScheduleType.INTERVAL) ? Integer.valueOf(schedule) : null;
        Job job = jobRepository.save(Job.builder()
                .title(title).description(description).targetUrl(targetUrl).jobExecutionType(jobExecutionType)
                .scheduleType(scheduleType).cronExpression(cronExpression).intervalSec(intervalSec).headerParameters(headerParameters)
                .bodyParameters(bodyParameters).queryParameters(queryParameters).member(member).build());
        return new JobDto.SaveResult(memberId, job.getId(), title);
    }
}
