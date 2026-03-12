package com.org.linkedin.utility.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publishEvent(String topic, Object event) {
    log.info("Publishing event to topic {}: {}", topic, event);
    kafkaTemplate.send(topic, event);
  }
}
