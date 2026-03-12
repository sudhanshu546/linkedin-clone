package com.org.linkedin.utility.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

  @Value("${kafka.topics.post-created:post-created}")
  private String postCreatedTopic;

  @Value("${kafka.topics.post-liked:post-liked}")
  private String postLikedTopic;

  @Value("${kafka.topics.comment-created:comment-created}")
  private String commentCreatedTopic;

  @Value("${kafka.topics.profile-viewed:profile-viewed}")
  private String profileViewedTopic;

  @Value("${kafka.topics.connection-requested:connection-requested}")
  private String connectionRequestedTopic;

  @Value("${kafka.topics.connection-accepted:connection-accepted}")
  private String connectionAcceptedTopic;

  @Bean
  public NewTopic postCreatedTopic() {
    return TopicBuilder.name(postCreatedTopic).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic postLikedTopic() {
    return TopicBuilder.name(postLikedTopic).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic commentCreatedTopic() {
    return TopicBuilder.name(commentCreatedTopic).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic profileViewedTopic() {
    return TopicBuilder.name(profileViewedTopic).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic connectionRequestedTopic() {
    return TopicBuilder.name(connectionRequestedTopic).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic connectionAcceptedTopic() {
    return TopicBuilder.name(connectionAcceptedTopic).partitions(3).replicas(1).build();
  }
}
