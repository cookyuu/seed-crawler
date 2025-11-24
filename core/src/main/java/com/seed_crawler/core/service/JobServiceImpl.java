package com.seed_crawler.core.service;

import com.seed_crawler.core.config.properties.ApplicationProperties;
import com.seed_crawler.core.converter.ScheduleConverter;
import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.dto.command.JobCreationCommand;
import com.seed_crawler.core.dto.command.JobDeleteCommand;
import com.seed_crawler.core.dto.command.JobOperationCommand;
import com.seed_crawler.core.dto.command.JobStatusCommand;
import com.seed_crawler.core.dto.command.JobUpdateCommand;
import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.entity.JobInfoHistory;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.global.context.UserContextManager;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.log.LogEvent;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.repository.JobInfoHistoryRepository;
import com.seed_crawler.core.repository.JobRepository;
import com.seed_crawler.core.repository.MemberRepository;
import com.seed_crawler.core.validator.JobValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final JobInfoHistoryRepository jobInfoHistoryRepository;
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

        Member modifier = memberRepository.findById(command.getMemberId())
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND, "회원 정보를 찾을 수 없습니다", "job.update.error", null));

        jobValidator.validateUrl(command.getTargetUrl());
        jobValidator.validateSchedule(command.getScheduleType(), command.getSchedule());
        if (command.getCallbackUrl() != null) {
            jobValidator.validateUrl(command.getCallbackUrl());
        }

        // 변경 전 데이터 저장
        Map<String, Object> beforeData = captureJobData(job);
        List<String> changedFields = new ArrayList<>();

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

        // 변경 필드 감지
        if (!job.getTitle().equals(command.getTitle())) changedFields.add("title");
        if (!nullSafeEquals(job.getDescription(), command.getDescription())) changedFields.add("description");
        if (!job.getTargetUrl().equals(command.getTargetUrl())) changedFields.add("targetUrl");
        if (job.getScheduleType() != command.getScheduleType()) changedFields.add("scheduleType");
        if (!nullSafeEquals(job.getCronExpression(), parsedSchedule.getCronExpression())) changedFields.add("cronExpression");
        if (!nullSafeEquals(job.getIntervalSec(), parsedSchedule.getIntervalSec())) changedFields.add("intervalSec");
        if (job.getJobExecutionType() != command.getJobExecutionType()) changedFields.add("jobExecutionType");
        if (job.getRetryLimit() != retryLimit) changedFields.add("retryLimit");
        if (job.getRetryIntervalSec() != retryIntervalSec) changedFields.add("retryIntervalSec");
        if (job.getTimeoutSec() != timeoutSec) changedFields.add("timeoutSec");
        if (!nullSafeEquals(job.getCallbackUrl(), command.getCallbackUrl())) changedFields.add("callbackUrl");

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

        // 변경 이력 저장
        if (!changedFields.isEmpty()) {
            Map<String, Object> afterData = captureJobData(job);
            String changeSummary = String.join(", ", changedFields) + " 변경";
            jobInfoHistoryRepository.save(JobInfoHistory.create(job, modifier, beforeData, afterData, changeSummary));
        }

        return new JobDto.UpdateResult(job.getId(), job.getTitle());
    }

    private Map<String, Object> captureJobData(Job job) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", job.getTitle());
        data.put("description", job.getDescription());
        data.put("targetUrl", job.getTargetUrl());
        data.put("scheduleType", job.getScheduleType() != null ? job.getScheduleType().name() : null);
        data.put("cronExpression", job.getCronExpression());
        data.put("intervalSec", job.getIntervalSec());
        data.put("jobExecutionType", job.getJobExecutionType() != null ? job.getJobExecutionType().name() : null);
        data.put("retryLimit", job.getRetryLimit());
        data.put("retryIntervalSec", job.getRetryIntervalSec());
        data.put("timeoutSec", job.getTimeoutSec());
        data.put("callbackUrl", job.getCallbackUrl());
        return data;
    }

    private boolean nullSafeEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
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

    @Override
    @LogEvent("job_operate")
    @Transactional
    public JobDto.OperationResult operateJob(JobOperationCommand command) {
        Job job = jobRepository.findById(command.getJobId())
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND, "Job을 찾을 수 없습니다", "job.operate.error", null));

        if (!job.isOwnedBy(command.getMemberId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "해당 Job에 대한 권한이 없습니다", "job.operate.error", null);
        }

        if (!job.isEnabled()) {
            throw new AppException(ErrorCode.JOB_DISABLED, "비활성화된 Job은 실행할 수 없습니다", "job.operate.error", null);
        }

        job.scheduleNextRun();

        return JobDto.OperationResult.builder()
                .jobId(job.getId())
                .title(job.getTitle())
                .status(job.getStatus().name())
                .build();
    }

    @Override
    @LogEvent("job_stop")
    @Transactional
    public JobDto.OperationResult stopJob(JobOperationCommand command) {
        Job job = jobRepository.findById(command.getJobId())
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND, "Job을 찾을 수 없습니다", "job.stop.error", null));

        if (!job.isOwnedBy(command.getMemberId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "해당 Job에 대한 권한이 없습니다", "job.stop.error", null);
        }

        job.stop();

        return JobDto.OperationResult.builder()
                .jobId(job.getId())
                .title(job.getTitle())
                .status(job.getStatus().name())
                .build();
    }
}
