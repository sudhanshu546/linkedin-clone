package com.org.linkedin.profile.consumer;

import com.org.linkedin.domain.Connection;
import com.org.linkedin.domain.Profile;
import com.org.linkedin.dto.event.UserDeletedEvent;
import com.org.linkedin.profile.repo.ConnectionRepository;
import com.org.linkedin.profile.repo.ProfileRepo;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDeletedConsumer {

  private final ProfileRepo profileRepo;
  private final ConnectionRepository connectionRepository;

  @KafkaListener(
      topics = "${kafka.topics.user-deleted}",
      groupId = "${spring.kafka.consumer.group-id}")
  @Transactional
  public void consumeUserDeleted(@Payload UserDeletedEvent event) {
    log.info("Received UserDeletedEvent for userId: {}", event.getUserId());
    try {
      UUID userId = UUID.fromString(event.getUserId());

      // Soft delete profile
      Profile profile = profileRepo.findByUserId(userId);
      if (profile != null) {
        profile.setIsDeleted(true);
        profileRepo.save(profile);
        log.info("Soft-deleted profile for userId: {}", userId);
      }

      // Soft delete connections where user is requester
      List<Connection> requested = connectionRepository.findByRequesterId(userId);
      for (Connection c : requested) {
        c.setIsDeleted(true);
      }
      connectionRepository.saveAll(requested);

      // Soft delete connections where user is receiver
      List<Connection> received = connectionRepository.findByReceiverId(userId);
      for (Connection c : received) {
        c.setIsDeleted(true);
      }
      connectionRepository.saveAll(received);

      log.info("Soft-deleted connections for userId: {}", userId);

    } catch (Exception e) {
      log.error("Error processing UserDeletedEvent in profile-service: {}", e.getMessage(), e);
    }
  }
}
