package com.seed_crawler.core.controller;

import com.seed_crawler.core.dto.Signup;
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
    public ResponseEntity<ApiResponse<Signup.Response>> signup(@RequestBody Signup.Request req, HttpServletRequest httpReq) {
        Signup.Result result = memberService.signup(req.getLoginId(), req.getPassword(), req.getNickname(), req.getEmail());
        Signup.Response payload = new Signup.Response(
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
