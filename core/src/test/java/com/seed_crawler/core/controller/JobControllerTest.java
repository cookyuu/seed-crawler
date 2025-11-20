package com.seed_crawler.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.ScheduleType;
import com.seed_crawler.core.global.auth.CustomUserDetails;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.exception.GlobalExceptionHandler;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.service.JobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobControllerTest {
    @InjectMocks
    private JobController jobController;

    @Mock
    private JobService jobService;

    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetails mockUser;
    private final UUID memberId = UUID.randomUUID();
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(jobController)
                .setControllerAdvice(new GlobalExceptionHandler()) // 프로젝트에 맞게
                .build();

        mockUser = new CustomUserDetails(memberId);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("성공: Job 등록 성공 → JSON 응답 구조 검증")
    void addJob_success() throws Exception {
        // Given
        UUID jobId = UUID.randomUUID();
        JobDto.SaveResult result = new JobDto.SaveResult(mockUser.getMemberId(), jobId, "test");

        when(jobService.saveJob(
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(result);

        JobDto.SaveRequest req = new JobDto.SaveRequest(
                "test", "desc", "https://test.com",
                JobExecutionType.API_JSON, ScheduleType.CRON, "0/10 * * * * *",
                Map.of(), Map.of(), Map.of(), 3, 10, 30, null
        );

        // When & Then
        mvc.perform(post("/api/job")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(() -> mockUser.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(memberId.toString()))
                .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.data.title").value("test"))
                .andExpect(jsonPath("$.message").value("크롤링 JOB 등록이 완료되었습니다."))
                .andExpect(jsonPath("$.messageCode").value("job.save.success"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("실패: MEMBER_NOT_FOUND 발생 시 에러 JSON 구조 검증")
    void addJob_memberNotFound_fail() throws Exception {
        // Given
        AppException exception = new AppException(
                ErrorCode.MEMBER_NOT_FOUND,
                "회원 정보를 찾을 수 없습니다",
                "job.save.error",
                null
        );

        when(jobService.saveJob(
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any()
        )).thenThrow(exception);

        JobDto.SaveRequest req = new JobDto.SaveRequest(
                "test", "desc", "https://test.com",
                JobExecutionType.API_JSON, ScheduleType.CRON, "0/10 * * * * *",
                Map.of(), Map.of(), Map.of(),
                3, 10, 30, null
        );

        // When & Then
        mvc.perform(post("/api/job")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(() -> mockUser.getUsername()))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("회원 정보를 찾을 수 없습니다"))
                .andExpect(jsonPath("$.error.messageCode").value("job.save.error"))
                .andExpect(jsonPath("$.error.status").value(404));
    }

}