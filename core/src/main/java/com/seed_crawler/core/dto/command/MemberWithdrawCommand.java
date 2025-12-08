package com.seed_crawler.core.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class MemberWithdrawCommand {
    private final UUID memberId;
    private final String password;
}
