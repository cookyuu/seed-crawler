package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.ScheduleType;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.repository.JobRepository;
import com.seed_crawler.core.repository.MemberRepository;
import com.seed_crawler.core.validator.JobValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    MemberRepository memberRepository;
    @Mock
    JobRepository jobRepository;
    @Mock
    JobValidator jobValidator;

    @InjectMocks
    JobServiceImpl jobService;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("성공: Job 저장 성공 시 repository/validator 모두 호출되고 SaveResult 반환됨")
    void saveJob_success() {
        // Given
        UUID memberId = UUID.randomUUID();
        Member member = new Member();
        ReflectionTestUtils.setField(member, "id", memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        Job savedJob = Job.builder()
                .title("test")
                .targetUrl("https://test.com")
                .build();
        ReflectionTestUtils.setField(savedJob, "id", UUID.randomUUID());

        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        // When
        JobDto.SaveResult result = jobService.saveJob(
                memberId,
                "test",
                "desc",
                "https://test.com",
                JobExecutionType.API_JSON,
                ScheduleType.CRON,
                "0/10 * * * * *",
                Map.of(),
                Map.of(),
                Map.of(),
                3,
                10,
                30,
                null
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getJobId()).isEqualTo(savedJob.getId());
        assertThat(result.getTitle()).isEqualTo("test");

        // Validator 호출 검증
        verify(jobValidator, times(1)).validateUrl("https://test.com");
        verify(jobValidator, times(1)).validateSchedule(ScheduleType.CRON, "0/10 * * * * *");

        // Repository 호출 검증
        verify(memberRepository, times(1)).findById(memberId);
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    @DisplayName("실패: Member가 존재하지 않을 경우 MEMBER_NOT_FOUND 예외 발생")
    void saveJob_memberNotFound_fail() {
        // Given
        UUID memberId = UUID.randomUUID();
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // When & Then
        AppException ex = catchThrowableOfType(() -> jobService.saveJob(
                memberId,
                "test",
                "desc",
                "https://test.com",
                JobExecutionType.API_JSON,
                ScheduleType.CRON,
                "0/10 * * * * *",
                Map.of(),
                Map.of(),
                Map.of(),
                3,
                10,
                30,
                null
        ), AppException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThat(ex.getMessage()).contains("회원 정보를 찾을 수 없습니다");

        verify(memberRepository, times(1)).findById(memberId);
        verify(jobValidator, never()).validateUrl(anyString());
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: URL 검증 실패 시 VALIDATION_ERROR 발생")
    void saveJob_urlValidationFail() {
        // Given
        UUID memberId = UUID.randomUUID();
        Member member = new Member();
        ReflectionTestUtils.setField(member, "id", memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        doThrow(new AppException(ErrorCode.VALIDATION_ERROR, "URL 주소값 검증 오류", "job.validation.error", null))
                .when(jobValidator).validateUrl("bad_url");

        // When
        AppException ex = catchThrowableOfType(() -> jobService.saveJob(
                memberId,
                "test",
                "desc",
                "bad_url",
                JobExecutionType.API_JSON,
                ScheduleType.CRON,
                "0/10 * * * * *",
                Map.of(),
                Map.of(),
                Map.of(),
                3, 10, 30, null
        ), AppException.class);

        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(ex.getMessage()).contains("URL 주소값 검증 오류");

        verify(memberRepository).findById(memberId);
        verify(jobValidator).validateUrl("bad_url");
        verify(jobRepository, never()).save(any());
    }
}