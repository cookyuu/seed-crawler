package com.seed_crawler.core.service;

import com.seed_crawler.core.config.properties.ApplicationProperties;
import com.seed_crawler.core.converter.ScheduleConverter;
import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.dto.command.JobCreationCommand;
import com.seed_crawler.core.dto.command.JobOperationCommand;
import com.seed_crawler.core.entity.Job;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.entity.enums.JobStatus;
import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.ScheduleType;
import com.seed_crawler.core.global.context.UserContextManager;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.repository.JobInfoHistoryRepository;
import com.seed_crawler.core.repository.JobRepository;
import com.seed_crawler.core.repository.MemberRepository;
import com.seed_crawler.core.validator.JobValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    JobInfoHistoryRepository jobInfoHistoryRepository;
    @Mock
    JobValidator jobValidator;
    @Mock
    ScheduleConverter scheduleConverter;
    @Mock
    ApplicationProperties appProperties;
    @Mock
    UserContextManager userContextManager;

    @InjectMocks
    JobServiceImpl jobService;

    @BeforeEach
    void setUp() {
        // ApplicationProperties Mock 설정
        ApplicationProperties.Job job = mock(ApplicationProperties.Job.class);
        ApplicationProperties.Job.Default defaults = mock(ApplicationProperties.Job.Default.class);

        lenient().when(appProperties.getJob()).thenReturn(job);
        lenient().when(job.getDefaults()).thenReturn(defaults);
        lenient().when(defaults.getRetryLimit()).thenReturn(3);
        lenient().when(defaults.getRetryIntervalSec()).thenReturn(60);
        lenient().when(defaults.getTimeoutSec()).thenReturn(30);

        // ScheduleConverter Mock 설정
        lenient().when(scheduleConverter.convert(any(), any())).thenAnswer(invocation -> {
            ScheduleType type = invocation.getArgument(0);
            String schedule = invocation.getArgument(1);
            if (type == ScheduleType.CRON) {
                return new ScheduleConverter.ParsedSchedule(schedule, null);
            } else {
                return new ScheduleConverter.ParsedSchedule(null, Integer.valueOf(schedule));
            }
        });
    }

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

        JobCreationCommand command = JobCreationCommand.builder()
                .memberId(memberId)
                .title("test")
                .description("desc")
                .targetUrl("https://test.com")
                .jobExecutionType(JobExecutionType.API_JSON)
                .scheduleType(ScheduleType.CRON)
                .schedule("0/10 * * * * *")
                .headerParameters(Map.of())
                .bodyParameters(Map.of())
                .queryParameters(Map.of())
                .retryLimit(3)
                .retryIntervalSec(10)
                .timeoutSec(30)
                .callbackUrl(null)
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        Job savedJob = Job.builder()
                .title("test")
                .targetUrl("https://test.com")
                .build();
        ReflectionTestUtils.setField(savedJob, "id", UUID.randomUUID());

        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        // When
        JobDto.SaveResult result = jobService.saveJob(command);

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

        JobCreationCommand command = JobCreationCommand.builder()
                .memberId(memberId)
                .title("test")
                .description("desc")
                .targetUrl("https://test.com")
                .jobExecutionType(JobExecutionType.API_JSON)
                .scheduleType(ScheduleType.CRON)
                .schedule("0/10 * * * * *")
                .headerParameters(Map.of())
                .bodyParameters(Map.of())
                .queryParameters(Map.of())
                .retryLimit(3)
                .retryIntervalSec(10)
                .timeoutSec(30)
                .callbackUrl(null)
                .build();

        // When & Then
        AppException ex = catchThrowableOfType(() -> jobService.saveJob(command), AppException.class);

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

        JobCreationCommand command = JobCreationCommand.builder()
                .memberId(memberId)
                .title("test")
                .description("desc")
                .targetUrl("bad_url")
                .jobExecutionType(JobExecutionType.API_JSON)
                .scheduleType(ScheduleType.CRON)
                .schedule("0/10 * * * * *")
                .headerParameters(Map.of())
                .bodyParameters(Map.of())
                .queryParameters(Map.of())
                .retryLimit(3)
                .retryIntervalSec(10)
                .timeoutSec(30)
                .callbackUrl(null)
                .build();

        // When
        AppException ex = catchThrowableOfType(() -> jobService.saveJob(command), AppException.class);

        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(ex.getMessage()).contains("URL 주소값 검증 오류");

        verify(memberRepository).findById(memberId);
        verify(jobValidator).validateUrl("bad_url");
        verify(jobRepository, never()).save(any());
    }

    // ==================== operateJob 테스트 ====================

    @Test
    @DisplayName("성공: Job 실행 예약 성공 시 상태가 SCHEDULED로 변경됨")
    void operateJob_success() {
        // Given
        UUID memberId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        Member member = new Member();
        ReflectionTestUtils.setField(member, "id", memberId);

        Job job = Job.builder()
                .title("Test Job")
                .targetUrl("https://test.com")
                .scheduleType(ScheduleType.INTERVAL)
                .intervalSec(300)
                .jobExecutionType(JobExecutionType.API_JSON)
                .retryLimit(3)
                .retryIntervalSec(60)
                .timeoutSec(30)
                .member(member)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        ReflectionTestUtils.setField(job, "enabled", true);
        ReflectionTestUtils.setField(job, "status", JobStatus.STOP);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        JobOperationCommand command = JobOperationCommand.builder()
                .jobId(jobId)
                .memberId(memberId)
                .build();

        // When
        JobDto.OperationResult result = jobService.operateJob(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(jobId);
        assertThat(result.getTitle()).isEqualTo("Test Job");
        assertThat(result.getStatus()).isEqualTo(JobStatus.SCHEDULED.name());
        assertThat(job.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(job.getNextRunAt()).isNotNull();
    }

    @Test
    @DisplayName("실패: 존재하지 않는 Job 실행 요청 시 JOB_NOT_FOUND 예외 발생")
    void operateJob_jobNotFound_fail() {
        // Given
        UUID memberId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        JobOperationCommand command = JobOperationCommand.builder()
                .jobId(jobId)
                .memberId(memberId)
                .build();

        // When & Then
        AppException ex = catchThrowableOfType(() -> jobService.operateJob(command), AppException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOB_NOT_FOUND);
        assertThat(ex.getMessage()).contains("Job을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("실패: 다른 사용자의 Job 실행 요청 시 FORBIDDEN 예외 발생")
    void operateJob_forbidden_fail() {
        // Given
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        Member owner = new Member();
        ReflectionTestUtils.setField(owner, "id", ownerId);

        Job job = Job.builder()
                .title("Test Job")
                .targetUrl("https://test.com")
                .scheduleType(ScheduleType.INTERVAL)
                .intervalSec(300)
                .jobExecutionType(JobExecutionType.API_JSON)
                .retryLimit(3)
                .retryIntervalSec(60)
                .timeoutSec(30)
                .member(owner)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        ReflectionTestUtils.setField(job, "enabled", true);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        JobOperationCommand command = JobOperationCommand.builder()
                .jobId(jobId)
                .memberId(requesterId)
                .build();

        // When & Then
        AppException ex = catchThrowableOfType(() -> jobService.operateJob(command), AppException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(ex.getMessage()).contains("권한이 없습니다");
    }

    @Test
    @DisplayName("실패: 비활성화된 Job 실행 요청 시 JOB_DISABLED 예외 발생")
    void operateJob_disabled_fail() {
        // Given
        UUID memberId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        Member member = new Member();
        ReflectionTestUtils.setField(member, "id", memberId);

        Job job = Job.builder()
                .title("Test Job")
                .targetUrl("https://test.com")
                .scheduleType(ScheduleType.INTERVAL)
                .intervalSec(300)
                .jobExecutionType(JobExecutionType.API_JSON)
                .retryLimit(3)
                .retryIntervalSec(60)
                .timeoutSec(30)
                .member(member)
                .build();
        ReflectionTestUtils.setField(job, "id", jobId);
        ReflectionTestUtils.setField(job, "enabled", false);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        JobOperationCommand command = JobOperationCommand.builder()
                .jobId(jobId)
                .memberId(memberId)
                .build();

        // When & Then
        AppException ex = catchThrowableOfType(() -> jobService.operateJob(command), AppException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOB_DISABLED);
        assertThat(ex.getMessage()).contains("비활성화된 Job은 실행할 수 없습니다");
    }
}