package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.MemberDto;
import com.seed_crawler.core.dto.command.MemberSignupCommand;
import com.seed_crawler.core.dto.command.MemberUpdateCommand;
import com.seed_crawler.core.dto.command.MemberWithdrawCommand;

public interface MemberService {
    MemberDto.Result signup(MemberSignupCommand command);
    MemberDto.UpdateResult updateMember(MemberUpdateCommand command);
    MemberDto.WithdrawResult withdraw(MemberWithdrawCommand command);
}
