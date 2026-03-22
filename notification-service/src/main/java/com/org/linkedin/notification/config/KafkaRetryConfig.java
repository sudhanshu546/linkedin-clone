package com.org.linkedin.notification.config;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;

@Configuration
public class KafkaRetryConfig {

  @Bean
  public RetryTopicConfiguration retryTopic(KafkaTemplate<String, Object> template) {
    return RetryTopicConfigurationBuilder.newInstance()
        .maxAttempts(3)
        .fixedBackOff(2000) // 2 seconds between retries
        .includeTopics(
            Arrays.asList(
                "connection-requested",
                "connection-accepted",
                "post-liked",
                "comment-created",
                "profile-viewed"))
        .create(template);
  }
}
