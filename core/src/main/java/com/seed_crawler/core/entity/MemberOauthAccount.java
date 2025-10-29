package com.seed_crawler.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.seed_crawler.core.entity.enums.OauthProvider;
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
public class MemberOauthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private OauthProvider provider;

    private String identificationId;

    private String accessToken;
    private String refreshToken;

    @CreatedDate
    private LocalDateTime linkedAt = LocalDateTime.now();
}
