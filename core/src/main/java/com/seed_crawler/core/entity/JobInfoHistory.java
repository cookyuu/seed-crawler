package com.seed_crawler.core.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class JobInfoHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member modifiedBy;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> beforeData;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> afterData;

    private String changeSummary;
    private LocalDateTime changedAt = LocalDateTime.now();

    public static JobInfoHistory create(Job job, Member modifiedBy, Map<String, Object> beforeData, Map<String, Object> afterData, String changeSummary) {
        JobInfoHistory history = new JobInfoHistory();
        history.job = job;
        history.modifiedBy = modifiedBy;
        history.beforeData = beforeData;
        history.afterData = afterData;
        history.changeSummary = changeSummary;
        history.changedAt = LocalDateTime.now();
        return history;
    }
}
