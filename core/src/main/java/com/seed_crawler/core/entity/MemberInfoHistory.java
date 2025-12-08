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
public class MemberInfoHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> beforeData;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> afterData;

    @Column(length = 255)
    private String changeSummary;

    private LocalDateTime changedAt = LocalDateTime.now();

    public static MemberInfoHistory create(Member member, Map<String, Object> beforeData, Map<String, Object> afterData, String changeSummary) {
        MemberInfoHistory history = new MemberInfoHistory();
        history.member = member;
        history.beforeData = beforeData;
        history.afterData = afterData;
        history.changeSummary = changeSummary;
        history.changedAt = LocalDateTime.now();
        return history;
    }
}
