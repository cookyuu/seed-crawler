package com.seed_crawler.core.controller;

import com.seed_crawler.core.dto.MemberDto;
import com.seed_crawler.core.dto.command.MemberSignupCommand;
import com.seed_crawler.core.global.response.ApiResponse;
import com.seed_crawler.core.global.response.ApiResponseFactory;
import com.seed_crawler.core.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
