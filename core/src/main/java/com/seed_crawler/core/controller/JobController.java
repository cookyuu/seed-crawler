package com.seed_crawler.core.controller;

import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.dto.command.JobCreationCommand;
import com.seed_crawler.core.dto.command.JobDeleteCommand;
import com.seed_crawler.core.dto.command.JobStatusCommand;
import com.seed_crawler.core.dto.command.JobUpdateCommand;
import com.seed_crawler.core.global.auth.CustomUserDetails;
import com.seed_crawler.core.global.response.ApiResponse;
import com.seed_crawler.core.global.response.ApiResponseFactory;
import com.seed_crawler.core.service.JobService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/job")
public class JobController {
    private final JobService jobService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobDto.SaveResponse>> addJob(@RequestBody JobDto.SaveRequest req, @AuthenticationPrincipal CustomUserDetails user, HttpServletRequest httpReq) {
        JobCreationCommand command = JobCreationCommand.builder()
                .memberId(user.getMemberId())
                .title(req.getTitle())
                .description(req.getDescription())
                .targetUrl(req.getTargetUrl())
                .jobExecutionType(req.getJobExecutionType())
                .scheduleType(req.getScheduleType())
                .schedule(req.getSchedule())
                .headerParameters(req.getHeaderParameters())
                .bodyParameters(req.getBodyParameters())
                .queryParameters(req.getQueryParameters())
                .retryLimit(req.getRetryLimit())
                .retryIntervalSec(req.getRetryIntervalSec())
                .timeoutSec(req.getTimeoutSec())
                .callbackUrl(req.getCallbackUrl())
                .build();

        JobDto.SaveResult result = jobService.saveJob(command);

        JobDto.SaveResponse payload = new JobDto.SaveResponse(
                result.getMemberId(),
                result.getJobId(),
                result.getTitle());

        var body = ApiResponseFactory.ok(
                payload,
                "크롤링 JOB 등록이 완료되었습니다.",
                "job.save.success",
                httpReq
        );
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobDto.UpdateResponse>> updateJob(
            @PathVariable UUID jobId,
            @RequestBody JobDto.UpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest httpReq) {

        JobUpdateCommand command = JobUpdateCommand.builder()
                .jobId(jobId)
                .memberId(user.getMemberId())
                .title(req.getTitle())
                .description(req.getDescription())
                .targetUrl(req.getTargetUrl())
                .jobExecutionType(req.getJobExecutionType())
                .scheduleType(req.getScheduleType())
                .schedule(req.getSchedule())
                .headerParameters(req.getHeaderParameters())
                .bodyParameters(req.getBodyParameters())
                .queryParameters(req.getQueryParameters())
                .retryLimit(req.getRetryLimit())
                .retryIntervalSec(req.getRetryIntervalSec())
                .timeoutSec(req.getTimeoutSec())
                .callbackUrl(req.getCallbackUrl())
                .build();

        JobDto.UpdateResult result = jobService.updateJob(command);

        JobDto.UpdateResponse payload = new JobDto.UpdateResponse(
                result.getJobId(),
                result.getTitle()
        );

        var body = ApiResponseFactory.ok(
                payload,
                "크롤링 JOB 수정이 완료되었습니다.",
                "job.update.success",
                httpReq
        );

        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobDto.DeleteResponse>> deleteJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest httpReq) {

        JobDeleteCommand command = JobDeleteCommand.builder()
                .jobId(jobId)
                .memberId(user.getMemberId())
                .build();

        JobDto.DeleteResult result = jobService.deleteJob(command);

        JobDto.DeleteResponse payload = new JobDto.DeleteResponse(result.getJobId());

        var body = ApiResponseFactory.ok(
                payload,
                "크롤링 JOB 삭제가 완료되었습니다.",
                "job.delete.success",
                httpReq
        );

        return ResponseEntity.ok(body);
    }

    @PatchMapping("/{jobId}/status")
    public ResponseEntity<ApiResponse<JobDto.StatusResponse>> updateJobStatus(
            @PathVariable UUID jobId,
            @RequestBody JobDto.StatusRequest req,
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest httpReq) {

        JobStatusCommand command = JobStatusCommand.builder()
                .jobId(jobId)
                .memberId(user.getMemberId())
                .enabled(req.isEnabled())
                .build();

        JobDto.StatusResult result = jobService.updateJobStatus(command);

        JobDto.StatusResponse payload = new JobDto.StatusResponse(
                result.getJobId(),
                result.isEnabled()
        );

        String message = req.isEnabled() ? "크롤링 JOB이 활성화되었습니다." : "크롤링 JOB이 비활성화되었습니다.";

        var body = ApiResponseFactory.ok(
                payload,
                message,
                "job.status.success",
                httpReq
        );

        return ResponseEntity.ok(body);
    }
}
