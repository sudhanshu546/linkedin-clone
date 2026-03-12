package com.org.linkedin.profile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import java.util.Arrays;

@Configuration
public class KafkaRetryConfig {

    @Bean
    public RetryTopicConfiguration retryTopic(KafkaTemplate<String, Object> template) {
        return RetryTopicConfigurationBuilder
                .newInstance()
                .maxAttempts(3)
                .fixedBackOff(5000) // 5 seconds between retries
                .includeTopics(Arrays.asList("post-created", "post-liked", "comment-created"))
                .create(template);
    }
}
