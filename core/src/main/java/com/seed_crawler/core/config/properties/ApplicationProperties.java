package com.seed_crawler.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 전역 설정을 관리하는 Properties 클래스
 * application.yml의 app.* 설정을 바인딩
 */
@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class ApplicationProperties {

    private Job job = new Job();
    private Cookie cookie = new Cookie();

    /**
     * Job 관련 설정
     */
    @Getter
    @Setter
    public static class Job {
        private Default defaults = new Default();

        /**
         * Job 기본값 설정
         */
        @Getter
        @Setter
        public static class Default {
            private int retryLimit = 3;
            private int retryIntervalSec = 60;
            private int timeoutSec = 30;
        }
    }

    /**
     * Cookie 보안 설정
     */
    @Getter
    @Setter
    public static class Cookie {
        private boolean secure = false;
        private String sameSite = "Lax";
        private String path = "/";
    }
}
