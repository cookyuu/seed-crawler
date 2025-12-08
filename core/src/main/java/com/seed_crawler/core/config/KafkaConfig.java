package com.seed_crawler.core.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String JOB_CRAWL_REQUEST_TOPIC = "job-crawl-request";
    public static final String JOB_CRAWL_RESULT_TOPIC = "job-crawl-result";

    @Bean
    public NewTopic jobCrawlRequestTopic() {
        return TopicBuilder.name(JOB_CRAWL_REQUEST_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic jobCrawlResultTopic() {
        return TopicBuilder.name(JOB_CRAWL_RESULT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
