package com.seed_crawler.core.global.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seed_crawler.core.global.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogEventAspect {

    private final HttpServletRequest request;

    @Around("@annotation(logEvent)")
    public Object logStructuredEvent(ProceedingJoinPoint joinPoint, LogEvent logEvent) throws Throwable {
        long start = System.currentTimeMillis();
        String event = logEvent.value();
        String status = "SUCCESS";
        String errorCode = null;

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (AppException e) {
            status = "FAILURE";
            errorCode = e.getErrorCode().name();
            throw e;
        } catch (Exception e) {
            status = "FAILURE";
            errorCode = "UNEXPECTED_ERROR";
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            logStructured(joinPoint, event, status, errorCode, duration);
        }
    }

    private void logStructured(ProceedingJoinPoint joinPoint,
                               String event,
                               String status,
                               String errorCode,
                               long duration) {

        // ✅ MDC 기반 공통 필드
        String traceId = MDC.get("traceId");
        String ip = MDC.get("ip");
        String userId = MDC.get("userId");

        // ✅ 요청 정보 (URI, HTTP METHOD)
        String uri = request.getRequestURI();
        String httpMethod = request.getMethod();

        // ✅ JSON 구조화 로그 구성
        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("timestamp", Instant.now().toString());
        logMap.put("level", status.equals("SUCCESS") ? "INFO" : "WARN");
        logMap.put("service", "auth-service");
        logMap.put("event", event);
        logMap.put("status", status);
        logMap.put("errorCode", errorCode);
        logMap.put("traceId", traceId);
        logMap.put("ip", ip);
        logMap.put("userId", userId);
        logMap.put("durationMs", duration);
        logMap.put("uri", uri);
        logMap.put("httpMethod", httpMethod);
        logMap.put("method", joinPoint.getSignature().toShortString());

        log.info("{}", toJson(logMap));
    }

    private String toJson(Map<String, Object> map) {
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return map.toString();
        }
    }
}
