package com.seed_crawler.core.entity.enums;

public enum FetchType {
    HTML,          // HTML 크롤링
    API_JSON,      // REST API (JSON)
    API_XML,       // REST API (XML)
    BROWSER,       // Selenium / Playwright
    FILE_DOWNLOAD  // FILE
}
