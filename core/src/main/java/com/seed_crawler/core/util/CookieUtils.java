package com.seed_crawler.core.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtils {
    public static void setCookieHttpOnly(HttpServletResponse httpRes, String key, String value, int maxAge) {
        Cookie refreshCookie = new Cookie(key, value);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(maxAge);
        httpRes.addCookie(refreshCookie);
    }
}
