package com.seed_crawler.core.entity;

import com.seed_crawler.core.entity.enums.JobExecutionType;
import com.seed_crawler.core.entity.enums.JobResponseType;
import com.seed_crawler.core.entity.enums.JobStatus;
import com.seed_crawler.core.entity.enums.ScheduleType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Job extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String title;
    private String description;
    private String targetUrl;
    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType;
    @Enumerated(EnumType.STRING)
    private JobExecutionType jobExecutionType;
    @Enumerated(EnumType.STRING)
    private JobResponseType responseType;
    private String cronExpression;
    private Integer intervalSec;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastRunAt;
    private LocalDateTime lastSuccessAt;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.STOP;

    private boolean enabled = true;

    private int failCount = 0;
    private int retryLimit;
    private int retryIntervalSec;
    private int timeoutSec;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> headerParameters;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> queryParameters;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> bodyParameters;

    private String callbackUrl;

    private int lastFetchedCount = 0;
    private long totalFetchedCount = 0;

    @Builder
    public Job(String title, String description, String targetUrl, ScheduleType scheduleType, String cronExpression, Integer intervalSec,
               JobExecutionType jobExecutionType, JobResponseType responseType, Map<String, Object> headerParameters, Map<String, Object> queryParameters,
               Map<String, Object> bodyParameters, int retryLimit, int retryIntervalSec, int timeoutSec, String callbackUrl, Member member) {
        this.title = title;
        this.description = description;
        this.targetUrl = targetUrl;
        this.scheduleType = scheduleType;
        this.jobExecutionType = jobExecutionType;
        this.responseType = responseType;
        this.cronExpression = cronExpression;
        this.intervalSec = intervalSec;
        this.headerParameters = headerParameters;
        this.queryParameters = queryParameters;
        this.bodyParameters = bodyParameters;
        this.callbackUrl = callbackUrl;
        this.retryLimit = retryLimit;
        this.retryIntervalSec = retryIntervalSec;
        this.timeoutSec = timeoutSec;
        this.enabled = true;
        this.status = JobStatus.STOP;
        this.member = member;
    }

    public void updateInfo(String title, String description, String targetUrl, ScheduleType scheduleType,
                           String cronExpression, Integer intervalSec, JobExecutionType jobExecutionType,
                           Map<String, Object> headerParameters, Map<String, Object> queryParameters,
                           Map<String, Object> bodyParameters, int retryLimit, int retryIntervalSec,
                           int timeoutSec, String callbackUrl) {
        this.title = title;
        this.description = description;
        this.targetUrl = targetUrl;
        this.scheduleType = scheduleType;
        this.cronExpression = cronExpression;
        this.intervalSec = intervalSec;
        this.jobExecutionType = jobExecutionType;
        this.headerParameters = headerParameters;
        this.queryParameters = queryParameters;
        this.bodyParameters = bodyParameters;
        this.retryLimit = retryLimit;
        this.retryIntervalSec = retryIntervalSec;
        this.timeoutSec = timeoutSec;
        this.callbackUrl = callbackUrl;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }

    public boolean isOwnedBy(UUID memberId) {
        return this.member.getId().equals(memberId);
    }

    public void updateStatus(JobStatus status) {
        this.status = status;
    }

    public void markAsRunning() {
        this.status = JobStatus.RUNNING;
        this.lastRunAt = LocalDateTime.now();
    }

    public void updateNextRunAt(LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public void scheduleNextRun() {
        this.status = JobStatus.SCHEDULED;
        calculateNextRunAt();
    }

    public void calculateNextRunAt() {
        LocalDateTime now = LocalDateTime.now();
        if (this.scheduleType == ScheduleType.INTERVAL && this.intervalSec != null) {
            this.nextRunAt = now.plusSeconds(this.intervalSec);
        } else if (this.scheduleType == ScheduleType.CRON && this.cronExpression != null) {
            this.nextRunAt = com.seed_crawler.core.util.CronUtils.getNextExecutionTime(this.cronExpression, now);
        }
    }

    public void completeRun() {
        this.lastRunAt = LocalDateTime.now();
        calculateNextRunAt();
        this.status = JobStatus.SCHEDULED;
    }

    public void stop() {
        this.status = JobStatus.STOP;
        this.nextRunAt = null;
    }
}
