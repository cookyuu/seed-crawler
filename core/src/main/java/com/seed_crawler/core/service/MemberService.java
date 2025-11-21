package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.MemberDto;
import com.seed_crawler.core.dto.command.MemberSignupCommand;

public interface MemberService {
    MemberDto.Result signup(MemberSignupCommand command);
}
