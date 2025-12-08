package com.seed_crawler.core.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class MemberUpdateCommand {
    private final UUID memberId;
    private final String nickname;
    private final String email;
    private final String currentPassword;
    private final String newPassword;
}
