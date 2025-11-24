package com.seed_crawler.core.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "검증 오류"),
    AUTH_INVALID_CREDENTIAL("AUTH_INVALID_CREDENTIAL", HttpStatus.UNAUTHORIZED, "계정 검증 오류"),
    AUTH_FORBIDDEN("AUTH_FORBIDDEN", HttpStatus.FORBIDDEN, "계정 접근제한"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "리소스 찾지 못함"),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT,"CONFLICT"),
    RATE_LIMIT("RATE_LIMIT", HttpStatus.TOO_MANY_REQUESTS, "RATE LIMIT"),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류"),
    DUPLICATE_REQUEST_EXCEPTION("DUPLICATE_REQUEST_EXCEPTION", HttpStatus.BAD_REQUEST, "입력값 오류"),

    INVALID_TOKEN("INVALID_TOKEN", HttpStatus.BAD_REQUEST, "토큰 검증 오류"),
    EXPIRED_TOKEN("EXPIRED_TOKEN", HttpStatus.UNAUTHORIZED, "토큰 만료 요류"),

    // Member
    INVALID_PASSWORD("INVALID_PASSWORD", HttpStatus.UNAUTHORIZED, "패스워드 불일치 오류"),
    AUTH_LOCKED("AUTH_LOCK", HttpStatus.FORBIDDEN, "계정 잠김 오류"),
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없음"),

    // Job
    JOB_NOT_FOUND("JOB_NOT_FOUND", HttpStatus.NOT_FOUND, "Job을 찾을 수 없음"),
    JOB_DISABLED("JOB_DISABLED", HttpStatus.BAD_REQUEST, "Job이 비활성화 상태"),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "권한이 없음")
    ;


    private final String code;
    private final HttpStatus status;
    private final String title;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.title = message;
    }
}
