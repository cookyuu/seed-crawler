package com.seed_crawler.core.entity;

import com.seed_crawler.core.entity.enums.MemberRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "member")
public class Member extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;

    private String nickname;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private MemberRole role = MemberRole.USER;

    private int pwFailCnt;
    private boolean accountLock = false;
    private boolean active = true;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Job> jobs = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<MemberActiveHistory> memberActiveHistories = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<MemberOauthAccount> memberOauthAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "executedBy", cascade = CascadeType.ALL)
    private List<JobWorkHistory> jobWorkHistories = new ArrayList<>();
}
