package com.seed_crawler.core.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class JobStatusCommand {
    private final UUID jobId;
    private final UUID memberId;
    private final boolean enabled;
}
