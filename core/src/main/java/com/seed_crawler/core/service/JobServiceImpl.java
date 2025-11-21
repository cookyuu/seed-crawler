package com.seed_crawler.core.service;

import com.seed_crawler.core.config.properties.ApplicationProperties;
import com.seed_crawler.core.converter.ScheduleConverter;
import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.dto.command.JobCreationCommand;
import com.seed_crawler.core.dto.command.JobDeleteCommand;
import com.seed_crawler.core.dto.command.JobStatusCommand;
import com.seed_crawler.core.dto.command.JobUpdateCommand;
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

    @Override
    @LogEvent("job_update")
    @Transactional
    public JobDto.UpdateResult updateJob(JobUpdateCommand command) {
        Job job = jobRepository.findById(command.getJobId())
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND, "Job을 찾을 수 없습니다", "job.update.error", null));

        if (!job.isOwnedBy(command.getMemberId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "해당 Job에 대한 권한이 없습니다", "job.update.error", null);
        }

        jobValidator.validateUrl(command.getTargetUrl());
        jobValidator.validateSchedule(command.getScheduleType(), command.getSchedule());
        if (command.getCallbackUrl() != null) {
            jobValidator.validateUrl(command.getCallbackUrl());
        }

        ScheduleConverter.ParsedSchedule parsedSchedule = scheduleConverter.convert(command.getScheduleType(), command.getSchedule());

        int retryLimit = (command.getRetryLimit() == null || command.getRetryLimit() == 0)
                ? appProperties.getJob().getDefaults().getRetryLimit()
                : command.getRetryLimit();
        int retryIntervalSec = (command.getRetryIntervalSec() == null || command.getRetryIntervalSec() == 0)
                ? appProperties.getJob().getDefaults().getRetryIntervalSec()
                : command.getRetryIntervalSec();
        int timeoutSec = (command.getTimeoutSec() == null || command.getTimeoutSec() == 0)
                ? appProperties.getJob().getDefaults().getTimeoutSec()
                : command.getTimeoutSec();

        job.updateInfo(
                command.getTitle(),
                command.getDescription(),
                command.getTargetUrl(),
                command.getScheduleType(),
                parsedSchedule.getCronExpression(),
                parsedSchedule.getIntervalSec(),
                command.getJobExecutionType(),
                command.getHeaderParameters(),
                command.getQueryParameters(),
                command.getBodyParameters(),
                retryLimit,
                retryIntervalSec,
                timeoutSec,
                command.getCallbackUrl()
        );

        return new JobDto.UpdateResult(job.getId(), job.getTitle());
    }

    @Override
    @LogEvent("job_delete")
    @Transactional
    public JobDto.DeleteResult deleteJob(JobDeleteCommand command) {
        Job job = jobRepository.findById(command.getJobId())
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND, "Job을 찾을 수 없습니다", "job.delete.error", null));

        if (!job.isOwnedBy(command.getMemberId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "해당 Job에 대한 권한이 없습니다", "job.delete.error", null);
        }

        jobRepository.delete(job);

        return new JobDto.DeleteResult(command.getJobId());
    }

    @Override
    @LogEvent("job_status_update")
    @Transactional
    public JobDto.StatusResult updateJobStatus(JobStatusCommand command) {
        Job job = jobRepository.findById(command.getJobId())
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND, "Job을 찾을 수 없습니다", "job.status.error", null));

        if (!job.isOwnedBy(command.getMemberId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "해당 Job에 대한 권한이 없습니다", "job.status.error", null);
        }

        if (command.isEnabled()) {
            job.enable();
        } else {
            job.disable();
        }

        return new JobDto.StatusResult(job.getId(), job.isEnabled());
    }
}
