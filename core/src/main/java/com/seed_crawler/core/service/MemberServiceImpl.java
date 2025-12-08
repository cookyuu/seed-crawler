package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.MemberDto;
import com.seed_crawler.core.dto.command.MemberSignupCommand;
import com.seed_crawler.core.dto.command.MemberUpdateCommand;
import com.seed_crawler.core.dto.command.MemberWithdrawCommand;
import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.entity.MemberInfoHistory;
import com.seed_crawler.core.global.context.UserContextManager;
import com.seed_crawler.core.global.exception.AppException;
import com.seed_crawler.core.global.log.LogEvent;
import com.seed_crawler.core.global.response.ErrorCode;
import com.seed_crawler.core.repository.MemberInfoHistoryRepository;
import com.seed_crawler.core.repository.MemberRepository;
import com.seed_crawler.core.validator.MemberValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final MemberInfoHistoryRepository memberInfoHistoryRepository;
    private final MemberValidator memberValidator;
    private final PasswordEncoder passwordEncoder;
    private final UserContextManager userContextManager;

    @Override
    @LogEvent("signup_attempt")
    @Transactional
    public MemberDto.Result signup(MemberSignupCommand command) {
        memberValidator.validateLoginId(command.getLoginId());
        memberValidator.validateEmail(command.getEmail());
        memberValidator.validatePassword(command.getPassword());

        if (memberRepository.existsByLoginId(command.getLoginId())) {
            throw new AppException(ErrorCode.DUPLICATE_REQUEST_EXCEPTION, "이미 등록된 로그인 아이디입니다.","member.signup.error",null);
        }
        if (memberRepository.existsByEmail(command.getEmail())) {
            throw new AppException(ErrorCode.DUPLICATE_REQUEST_EXCEPTION, "이미 등록된 이메일입니다.","member.signup.error",null);
        }
        String encodedPw = passwordEncoder.encode(command.getPassword());
        Member member = memberRepository.save(
                Member.builder()
                        .loginId(command.getLoginId())
                        .password(encodedPw)
                        .nickname(command.getNickname())
                        .email(command.getEmail())
                        .build()
        );
        userContextManager.setUserId(member.getId());
        return new MemberDto.Result(member.getId(), member.getLoginId(), member.getNickname(), member.getEmail());
    }

    @Override
    @LogEvent("member_update")
    @Transactional
    public MemberDto.UpdateResult updateMember(MemberUpdateCommand command) {
        Member member = memberRepository.findById(command.getMemberId())
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND, "회원 정보를 찾을 수 없습니다", "member.update.error", null));

        // 변경 전 데이터 저장
        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("nickname", member.getNickname());
        beforeData.put("email", member.getEmail());

        List<String> changedFields = new ArrayList<>();

        if (command.getEmail() != null && !command.getEmail().equals(member.getEmail())) {
            memberValidator.validateEmail(command.getEmail());
            if (memberRepository.existsByEmail(command.getEmail())) {
                throw new AppException(ErrorCode.DUPLICATE_REQUEST_EXCEPTION, "이미 등록된 이메일입니다.", "member.update.error", null);
            }
            changedFields.add("email");
        }

        if (command.getNickname() != null && !command.getNickname().equals(member.getNickname())) {
            changedFields.add("nickname");
        }

        boolean passwordChanged = false;
        if (command.getNewPassword() != null && !command.getNewPassword().isEmpty()) {
            if (!passwordEncoder.matches(command.getCurrentPassword(), member.getPassword())) {
                throw new AppException(ErrorCode.INVALID_PASSWORD, "현재 비밀번호가 일치하지 않습니다", "member.update.error", null);
            }
            memberValidator.validatePassword(command.getNewPassword());
            member.updatePassword(passwordEncoder.encode(command.getNewPassword()));
            passwordChanged = true;
            changedFields.add("password");
        }

        member.updateInfo(
                command.getNickname() != null ? command.getNickname() : member.getNickname(),
                command.getEmail() != null ? command.getEmail() : member.getEmail()
        );

        // 변경 후 데이터 저장 및 이력 기록
        if (!changedFields.isEmpty()) {
            Map<String, Object> afterData = new HashMap<>();
            afterData.put("nickname", member.getNickname());
            afterData.put("email", member.getEmail());
            if (passwordChanged) {
                beforeData.put("password", "******");
                afterData.put("password", "******");
            }

            String changeSummary = String.join(", ", changedFields) + " 변경";
            memberInfoHistoryRepository.save(MemberInfoHistory.create(member, beforeData, afterData, changeSummary));
        }

        return new MemberDto.UpdateResult(member.getId(), member.getNickname(), member.getEmail());
    }

    @Override
    @LogEvent("member_withdraw")
    @Transactional
    public MemberDto.WithdrawResult withdraw(MemberWithdrawCommand command) {
        Member member = memberRepository.findById(command.getMemberId())
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND, "회원 정보를 찾을 수 없습니다", "member.withdraw.error", null));

        if (!passwordEncoder.matches(command.getPassword(), member.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD, "비밀번호가 일치하지 않습니다", "member.withdraw.error", null);
        }

        member.withdraw();

        return new MemberDto.WithdrawResult(member.getId());
    }
}