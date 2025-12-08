package com.seed_crawler.core.controller;

import com.seed_crawler.core.dto.MemberDto;
import com.seed_crawler.core.dto.command.MemberSignupCommand;
import com.seed_crawler.core.dto.command.MemberUpdateCommand;
import com.seed_crawler.core.dto.command.MemberWithdrawCommand;
import com.seed_crawler.core.global.auth.CustomUserDetails;
import com.seed_crawler.core.global.response.ApiResponse;
import com.seed_crawler.core.global.response.ApiResponseFactory;
import com.seed_crawler.core.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberDto.SignupResponse>> signup(@RequestBody MemberDto.SignupRequest req, HttpServletRequest httpReq) {
        MemberSignupCommand command = MemberSignupCommand.builder()
                .loginId(req.getLoginId())
                .password(req.getPassword())
                .nickname(req.getNickname())
                .email(req.getEmail())
                .build();

        MemberDto.Result result = memberService.signup(command);

        MemberDto.SignupResponse payload = new MemberDto.SignupResponse(
                result.getMemberId(),
                result.getLoginId(),
                result.getNickname(),
                result.getEmail()
        );

        var body = ApiResponseFactory.ok(
                payload,
                "회원가입이 완료되었습니다.",
                "member.signup.success",
                httpReq
        );

        return ResponseEntity.ok(body);
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MemberDto.UpdateResponse>> updateMember(
            @RequestBody MemberDto.UpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest httpReq) {

        MemberUpdateCommand command = MemberUpdateCommand.builder()
                .memberId(user.getMemberId())
                .nickname(req.getNickname())
                .email(req.getEmail())
                .currentPassword(req.getCurrentPassword())
                .newPassword(req.getNewPassword())
                .build();

        MemberDto.UpdateResult result = memberService.updateMember(command);

        MemberDto.UpdateResponse payload = new MemberDto.UpdateResponse(
                result.getMemberId(),
                result.getNickname(),
                result.getEmail()
        );

        var body = ApiResponseFactory.ok(
                payload,
                "회원정보가 수정되었습니다.",
                "member.update.success",
                httpReq
        );

        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<MemberDto.WithdrawResponse>> withdraw(
            @RequestBody MemberDto.WithdrawRequest req,
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest httpReq) {

        MemberWithdrawCommand command = MemberWithdrawCommand.builder()
                .memberId(user.getMemberId())
                .password(req.getPassword())
                .build();

        MemberDto.WithdrawResult result = memberService.withdraw(command);

        MemberDto.WithdrawResponse payload = new MemberDto.WithdrawResponse(result.getMemberId());

        var body = ApiResponseFactory.ok(
                payload,
                "회원탈퇴가 완료되었습니다.",
                "member.withdraw.success",
                httpReq
        );

        return ResponseEntity.ok(body);
    }
}
