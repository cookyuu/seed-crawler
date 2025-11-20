package com.seed_crawler.core.controller;

import com.seed_crawler.core.dto.AuthDto;
import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.global.auth.CustomUserDetails;
import com.seed_crawler.core.global.response.ApiResponse;
import com.seed_crawler.core.global.response.ApiResponseFactory;
import com.seed_crawler.core.service.JobService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/job")
public class JobController {
    private final JobService jobService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobDto.SaveResponse>> addJob(@RequestBody JobDto.SaveRequest req, @AuthenticationPrincipal CustomUserDetails user, HttpServletRequest httpReq) {
        UUID memberId = user.getMemberId();
        JobDto.SaveResult result = jobService.saveJob(
                memberId, req.getTitle(), req.getDescription(), req.getTargetUrl(), req.getJobExecutionType(), req.getScheduleType(),
                req.getSchedule(), req.getHeaderParameters(), req.getBodyParameters(), req.getQueryParameters(), req.getRetryLimit(),
                req.getRetryIntervalSec(), req.getTimeoutSec(), req.getCallbackUrl());
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
}
