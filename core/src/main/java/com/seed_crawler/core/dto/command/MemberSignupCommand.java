package com.seed_crawler.core.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MemberSignupCommand {
    private final String loginId;
    private final String password;
    private final String nickname;
    private final String email;
}
