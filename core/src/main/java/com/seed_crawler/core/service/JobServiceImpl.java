package com.seed_crawler.core.service;

import com.seed_crawler.core.config.properties.ApplicationProperties;
import com.seed_crawler.core.converter.ScheduleConverter;
import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.dto.command.JobCreationCommand;
import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.global.context.UserContextManager;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.log.LogEvent;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.repository.JobRepository;
import com.seed_crawler.core.repository.MemberRepository;
import com.seed_crawler.core.validator.JobValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final MemberRepository memberRepository;
    private final JobValidator jobValidator;
    private final ScheduleConverter scheduleConverter;
    private final ApplicationProperties appProperties;
    private final UserContextManager userContextManager;

    @Override
    @LogEvent("job_save")
    @Transactional
    public JobDto.SaveResult saveJob(JobCreationCommand command) {
        Member member = memberRepository.findById(command.getMemberId())
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND,"회원 정보를 찾을 수 없습니다","job.save.error",null));

        jobValidator.validateUrl(command.getTargetUrl());
        jobValidator.validateSchedule(command.getScheduleType(), command.getSchedule());
        if (command.getCallbackUrl() != null) {
            jobValidator.validateUrl(command.getCallbackUrl());
        }

        ScheduleConverter.ParsedSchedule parsedSchedule = scheduleConverter.convert(command.getScheduleType(), command.getSchedule());

        // 기본값 적용: 0이거나 null인 경우 설정 파일의 기본값 사용
        int retryLimit = (command.getRetryLimit() == null || command.getRetryLimit() == 0)
                ? appProperties.getJob().getDefaults().getRetryLimit()
                : command.getRetryLimit();
        int retryIntervalSec = (command.getRetryIntervalSec() == null || command.getRetryIntervalSec() == 0)
                ? appProperties.getJob().getDefaults().getRetryIntervalSec()
                : command.getRetryIntervalSec();
        int timeoutSec = (command.getTimeoutSec() == null || command.getTimeoutSec() == 0)
                ? appProperties.getJob().getDefaults().getTimeoutSec()
                : command.getTimeoutSec();

        Job job = jobRepository.save(Job.builder()
                .title(command.getTitle())
                .description(command.getDescription())
                .targetUrl(command.getTargetUrl())
                .jobExecutionType(command.getJobExecutionType())
                .scheduleType(command.getScheduleType())
                .cronExpression(parsedSchedule.getCronExpression())
                .intervalSec(parsedSchedule.getIntervalSec())
                .headerParameters(command.getHeaderParameters())
                .bodyParameters(command.getBodyParameters())
                .queryParameters(command.getQueryParameters())
                .retryLimit(retryLimit)
                .retryIntervalSec(retryIntervalSec)
                .timeoutSec(timeoutSec)
                .member(member)
                .build());
        return new JobDto.SaveResult(command.getMemberId(), job.getId(), command.getTitle());
    }
}
