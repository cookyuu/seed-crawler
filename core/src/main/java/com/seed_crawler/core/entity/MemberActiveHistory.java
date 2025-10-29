package com.seed_crawler.core.entity;

import com.seed_crawler.core.entity.enums.MemberActivityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MemberActiveHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberActivityType activityType;

    @Column
    private String ipAddress;

    @Column
    private String userAgent;

    @Column
    private boolean success;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();
}
