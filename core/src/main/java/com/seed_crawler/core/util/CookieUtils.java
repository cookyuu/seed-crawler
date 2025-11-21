package com.seed_crawler.core.util;

import com.seed_crawler.core.config.properties.ApplicationProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieUtils {

    private final ApplicationProperties appProperties;

    public void setCookieHttpOnly(HttpServletResponse httpRes, String key, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(key, value)
                .httpOnly(true)
                .secure(appProperties.getCookie().isSecure())
                .path(appProperties.getCookie().getPath())
                .maxAge(Duration.ofSeconds(maxAge))
                .sameSite(appProperties.getCookie().getSameSite())
                .build();

        httpRes.addHeader("Set-Cookie", cookie.toString());
    }
}
