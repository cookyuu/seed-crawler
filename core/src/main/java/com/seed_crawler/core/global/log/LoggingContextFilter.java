package com.seed_crawler.core.global.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


@Slf4j
@Component
public class LoggingContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().substring(0, 12);
        MDC.put("traceId", traceId);
        MDC.put("ip", request.getRemoteAddr());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
