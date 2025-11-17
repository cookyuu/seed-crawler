package com.seed_crawler.core.global.exception;

import com.seed_crawler.core.global.response.ApiResponse;
import com.seed_crawler.core.global.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String API_VERSION = "1.0.0";

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex, HttpServletRequest req) {
        var binding = (ex instanceof MethodArgumentNotValidException manv)
                ? manv.getBindingResult()
                : ((BindException) ex).getBindingResult();

        var details = binding.getFieldErrors().stream()
                .map(fe -> ApiResponse.ApiError.Detail.builder()
                        .field(fe.getField())
                        .reason(fe.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        var error = ApiResponse.ApiError.builder()
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .title("입력값 검증 실패")
                .message("요청 파라미터를 확인해 주세요.")
                .messageCode("common.validation.failed")
                .status(ErrorCode.VALIDATION_ERROR.getStatus().value())
                .details(details)
                .build();

        var meta = ApiResponse.Meta.builder()
                .apiVersion(API_VERSION)
                .timestamp(OffsetDateTime.now())
                .requestId(getOrGenRequestId(req))
                .path(req.getRequestURI())
                .locale(LocaleContextHolder.getLocale().toLanguageTag())
                .build();

        var body = ApiResponse.<Void>builder()
                .success(false)
                .error(error)
                .meta(meta)
                .build();

        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus()).body(body);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(Exception ex, HttpServletRequest req) {
        var exMessage = ex.getMessage();
        var error = ApiResponse.ApiError.builder()
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .title("서버 오류")
                .message(exMessage)
                .messageCode("common.server.error")
                .status(ErrorCode.INTERNAL_ERROR.getStatus().value())
                .details(null)
                .build();

        var meta = ApiResponse.Meta.builder()
                .apiVersion(API_VERSION)
                .timestamp(OffsetDateTime.now())
                .requestId(getOrGenRequestId(req))
                .path(req.getRequestURI())
                .locale(LocaleContextHolder.getLocale().toLanguageTag())
                .build();

        var body = ApiResponse.<Void>builder()
                .success(false)
                .error(error)
                .meta(meta)
                .build();

        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus()).body(body);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleApp(AppException ex, HttpServletRequest req) {
        var errCode = ex.getErrorCode();
        var error = ApiResponse.ApiError.builder()
                .code(errCode.getCode())
                .title(errCode.getTitle())
                .message(ex.getMessage())
                .messageCode(ex.getMessageCode())
                .status(errCode.getStatus().value())
                .details(ex.getDetails())
                .build();

        var meta = ApiResponse.Meta.builder()
                .apiVersion(API_VERSION)
                .timestamp(OffsetDateTime.now())
                .requestId(getOrGenRequestId(req))
                .path(req.getRequestURI())
                .locale(LocaleContextHolder.getLocale().toLanguageTag())
                .build();

        var body = ApiResponse.<Void>builder()
                .success(false)
                .error(error)
                .meta(meta)
                .build();

        return ResponseEntity.status(errCode.getStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex, HttpServletRequest req) {
        // 로그 남기고(생략)
        var error = ApiResponse.ApiError.builder()
                .code(ErrorCode.INTERNAL_ERROR.getCode())
                .title("서버 오류")
                .message("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.")
                .messageCode("common.internal.error")
                .status(ErrorCode.INTERNAL_ERROR.getStatus().value())
                .build();

        var meta = ApiResponse.Meta.builder()
                .apiVersion(API_VERSION)
                .timestamp(OffsetDateTime.now())
                .requestId(getOrGenRequestId(req))
                .path(req.getRequestURI())
                .locale(LocaleContextHolder.getLocale().toLanguageTag())
                .build();

        var body = ApiResponse.<Void>builder()
                .success(false)
                .error(error)
                .meta(meta)
                .build();

        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus()).body(body);
    }

    private String getOrGenRequestId(HttpServletRequest req) {
        var rid = req.getHeader("X-Request-Id");
        return (rid == null || rid.isBlank()) ? UUID.randomUUID().toString() : rid;
    }
}
