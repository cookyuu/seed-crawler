package com.seed_crawler.core.global.context;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/*
 * 사용자 컨텍스트를 관리하는 유틸리티 클래스
 */
@Component
public class UserContextManager {

    private static final String USER_ID_KEY = "userId";

    public void setUserId(UUID userId) {
        if (userId != null) {
            MDC.put(USER_ID_KEY, userId.toString());
        }
    }

    public void clearUserId() {
        MDC.remove(USER_ID_KEY);
    }

    public void clearAll() {
        MDC.clear();
    }
}
