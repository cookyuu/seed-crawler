package com.seed_crawler.core.global.response;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class ApiResponse<T> {
    boolean success;
    T data;                // 성공 시
    ApiError error;        // 실패 시
    String message;        // 사용자 메시지(로컬라이즈 전송 가능)
    String messageCode;    // i18n 키
    Meta meta;
    Links links;

    @Value
    @Builder
    public static class Meta {
        String apiVersion;
        OffsetDateTime timestamp;
        String requestId;
        String path;
        String locale;
        // 페이징용
        Integer page;
        Integer size;
        Long totalElements;
        Integer totalPages;
        String sorted;
        Map<String, Object> extra; // 확장 필드
    }

    @Value
    @Builder
    public static class Links {
        String self;
        String next;
        String prev;
    }

    @Value
    @Builder
    public static class ApiError {
        String code;          // 사내 오류 코드
        String title;         // 짧은 제목
        String message;       // 사용자 메시지
        String messageCode;   // i18n 키
        Integer status;       // HTTP status
        List<Detail> details; // 필드 단위 오류 등

        @Value
        @Builder
        public static class Detail {
            String field;
            String reason;
        }
    }
}