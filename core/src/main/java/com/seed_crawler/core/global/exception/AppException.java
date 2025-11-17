package com.seed_crawler.core.global.exception;

import com.seed_crawler.core.global.response.ApiResponse;
import com.seed_crawler.core.global.response.ErrorCode;
import lombok.Getter;

import java.util.List;

@Getter
public class AppException extends RuntimeException{
    private final ErrorCode errorCode;
    private final String messageCode;
    private final List<ApiResponse.ApiError.Detail> details;

    public AppException(ErrorCode errorCode, String message, String messageCode,
                        List<ApiResponse.ApiError.Detail> details) {
        super(message);
        this.errorCode = errorCode;
        this.messageCode = messageCode;
        this.details = details;
    }

    public static AppException of(ErrorCode code,  String message, String messageCode) {
        return new AppException(code, message, messageCode, null);
    }
}
