package com.seed_crawler.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.MemberRole;
import com.seed_crawler.core.entity.enums.ScheduleType;
import com.seed_crawler.core.global.auth.CustomUserDetails;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.service.JobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobService jobService;

    private CustomUserDetails mockUser;
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockUser = new CustomUserDetails(memberId, MemberRole.USER, false, true);
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

        when(jobService.saveJob(any())).thenReturn(result);

        JobDto.SaveRequest req = new JobDto.SaveRequest(
                "test", "desc", "https://test.com",
                JobExecutionType.API_JSON, ScheduleType.CRON, "0/10 * * * * *",
                Map.of(), Map.of(), Map.of(), 3, 10, 30, null
        );

        // When & Then
        mvc.perform(post("/api/job")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(mockUser)))
                .andDo(print())
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

        when(jobService.saveJob(any())).thenThrow(exception);

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
                        .with(user(mockUser)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("회원 정보를 찾을 수 없습니다"))
                .andExpect(jsonPath("$.error.messageCode").value("job.save.error"))
                .andExpect(jsonPath("$.error.status").value(404));
    }

    @Test
    @DisplayName("성공: Job 수정 성공 → JSON 응답 구조 검증")
    void updateJob_success() throws Exception {
        // Given
        UUID jobId = UUID.randomUUID();
        JobDto.UpdateResult result = new JobDto.UpdateResult(jobId, "updated test");

        when(jobService.updateJob(any())).thenReturn(result);

        JobDto.UpdateRequest req = new JobDto.UpdateRequest(
                "updated test", "updated desc", "https://updated.com",
                JobExecutionType.API_JSON, ScheduleType.CRON, "0/20 * * * * *",
                Map.of(), Map.of(), Map.of(), 5, 20, 60, null
        );

        // When & Then
        mvc.perform(put("/api/job/{jobId}", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.data.title").value("updated test"))
                .andExpect(jsonPath("$.message").value("크롤링 JOB 수정이 완료되었습니다."))
                .andExpect(jsonPath("$.messageCode").value("job.update.success"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("실패: Job 수정 시 JOB_NOT_FOUND 발생 → 에러 JSON 구조 검증")
    void updateJob_jobNotFound_fail() throws Exception {
        // Given
        UUID jobId = UUID.randomUUID();
        AppException exception = new AppException(
                ErrorCode.JOB_NOT_FOUND,
                "Job을 찾을 수 없습니다",
                "job.update.error",
                null
        );

        when(jobService.updateJob(any())).thenThrow(exception);

        JobDto.UpdateRequest req = new JobDto.UpdateRequest(
                "updated test", "updated desc", "https://updated.com",
                JobExecutionType.API_JSON, ScheduleType.CRON, "0/20 * * * * *",
                Map.of(), Map.of(), Map.of(), 5, 20, 60, null
        );

        // When & Then
        mvc.perform(put("/api/job/{jobId}", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(mockUser)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("JOB_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Job을 찾을 수 없습니다"))
                .andExpect(jsonPath("$.error.messageCode").value("job.update.error"))
                .andExpect(jsonPath("$.error.status").value(404));
    }

    @Test
    @DisplayName("성공: Job 삭제 성공 → JSON 응답 구조 검증")
    void deleteJob_success() throws Exception {
        // Given
        UUID jobId = UUID.randomUUID();
        JobDto.DeleteResult result = new JobDto.DeleteResult(jobId);

        when(jobService.deleteJob(any())).thenReturn(result);

        // When & Then
        mvc.perform(delete("/api/job/{jobId}", jobId)
                        .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.message").value("크롤링 JOB 삭제가 완료되었습니다."))
                .andExpect(jsonPath("$.messageCode").value("job.delete.success"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("실패: Job 삭제 시 FORBIDDEN 발생 → 에러 JSON 구조 검증")
    void deleteJob_forbidden_fail() throws Exception {
        // Given
        UUID jobId = UUID.randomUUID();
        AppException exception = new AppException(
                ErrorCode.FORBIDDEN,
                "해당 Job에 대한 권한이 없습니다",
                "job.delete.error",
                null
        );

        when(jobService.deleteJob(any())).thenThrow(exception);

        // When & Then
        mvc.perform(delete("/api/job/{jobId}", jobId)
                        .with(user(mockUser)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("해당 Job에 대한 권한이 없습니다"))
                .andExpect(jsonPath("$.error.messageCode").value("job.delete.error"))
                .andExpect(jsonPath("$.error.status").value(403));
    }

    @Test
    @DisplayName("성공: Job 비활성화 성공 → JSON 응답 구조 검증")
    void updateJobStatus_disable_success() throws Exception {
        // Given
        UUID jobId = UUID.randomUUID();
        JobDto.StatusResult result = new JobDto.StatusResult(jobId, false);

        when(jobService.updateJobStatus(any())).thenReturn(result);

        JobDto.StatusRequest req = new JobDto.StatusRequest(false);

        // When & Then
        mvc.perform(patch("/api/job/{jobId}/status", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.message").value("크롤링 JOB이 비활성화되었습니다."))
                .andExpect(jsonPath("$.messageCode").value("job.status.success"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("성공: Job 활성화 성공 → JSON 응답 구조 검증")
    void updateJobStatus_enable_success() throws Exception {
        // Given
        UUID jobId = UUID.randomUUID();
        JobDto.StatusResult result = new JobDto.StatusResult(jobId, true);

        when(jobService.updateJobStatus(any())).thenReturn(result);

        JobDto.StatusRequest req = new JobDto.StatusRequest(true);

        // When & Then
        mvc.perform(patch("/api/job/{jobId}/status", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.message").value("크롤링 JOB이 활성화되었습니다."))
                .andExpect(jsonPath("$.messageCode").value("job.status.success"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

}