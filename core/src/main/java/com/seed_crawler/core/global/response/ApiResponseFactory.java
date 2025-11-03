package com.seed_crawler.core.global.response;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ApiResponseFactory {
    private static final String API_VERSION = "1.0.0";

    public static <T> ApiResponse<T> ok(T data, String message, String messageCode,
                                        HttpServletRequest req) {
        var meta = ApiResponse.Meta.builder()
                .apiVersion(API_VERSION)
                .timestamp(OffsetDateTime.now())
                .requestId(getOrGenRequestId(req))
                .path(req.getRequestURI())
                .locale(LocaleContextHolder.getLocale().toLanguageTag())
                .build();

        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .messageCode(messageCode)
                .meta(meta)
                .build();
    }

    private static String getOrGenRequestId(HttpServletRequest req) {
        var rid = req.getHeader("X-Request-Id");
        return (rid == null || rid.isBlank()) ? UUID.randomUUID().toString() : rid;
    }
}
