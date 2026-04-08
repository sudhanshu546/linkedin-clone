package com.org.linkedin.profile.service.impl;

import com.org.linkedin.domain.Connection;
import com.org.linkedin.domain.enumeration.ConnectionStatus;
import com.org.linkedin.dto.connection.ConnectionDTO;
import com.org.linkedin.dto.connection.UserConnectionStatusDTO;
import com.org.linkedin.dto.event.ConnectionRequestedEvent;
import com.org.linkedin.profile.mapper.ConnectionMapper;
import com.org.linkedin.profile.repo.ConnectionRepository;
import com.org.linkedin.profile.service.ConnectionService;
import com.org.linkedin.utility.client.UserService;
import com.org.linkedin.utility.service.KafkaEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionServiceImpl implements ConnectionService {
  private final UserService userService;
  private final ConnectionRepository repository;
  private final KafkaEventPublisher kafkaEventPublisher;
  private final ConnectionMapper connectionMapper;

  @Value("${kafka.topics.connection-requested}")
  private String connectionRequestedTopic;

  @Value("${kafka.topics.connection-accepted}")
  private String connectionAcceptedTopic;

  @Override
  public void sendRequest(Authentication authentication, UUID receiverId) {
    UUID requesterId =
        userService
            .getUserByKeyCloakId(UUID.fromString(authentication.getName()))
            .getBody()
            .getData()
            .getId();
    log.info("Sending connection request from {} to {}", requesterId, receiverId);

    if (requesterId.equals(receiverId)) {
      throw new RuntimeException("Cannot connect with yourself");
    }

    boolean alreadyExists =
        repository.findByRequesterIdAndReceiverId(requesterId, receiverId).isPresent()
            || repository.findByRequesterIdAndReceiverId(receiverId, requesterId).isPresent();

    if (alreadyExists) {
      throw new RuntimeException("Connection request already exists or you are already connected");
    }

    Connection connection = new Connection();
    connection.setRequesterId(requesterId);
    connection.setReceiverId(receiverId);
    connection.setStatus(ConnectionStatus.PENDING);
    repository.save(connection);

    ConnectionRequestedEvent event =
        ConnectionRequestedEvent.builder()
            .senderId(requesterId)
            .receiverId(receiverId)
            .timestamp(LocalDateTime.now())
            .build();
    kafkaEventPublisher.publishEvent(connectionRequestedTopic, event);
  }

  @Override
  public void respondToRequest(Authentication authentication, UUID connectionId, boolean accept) {
    UUID userId =
        userService
            .getUserByKeyCloakId(UUID.fromString(authentication.getName()))
            .getBody()
            .getData()
            .getId();
    Connection connection =
        repository
            .findById(connectionId)
            .orElseThrow(() -> new RuntimeException("Request not found"));

    if (!connection.getReceiverId().equals(userId)) {
      throw new RuntimeException("Unauthorized");
    }

    connection.setStatus(accept ? ConnectionStatus.ACCEPTED : ConnectionStatus.REJECTED);
    repository.save(connection);

    if (accept) {
      com.org.linkedin.dto.event.ConnectionAcceptedEvent event =
          com.org.linkedin.dto.event.ConnectionAcceptedEvent.builder()
              .requesterId(connection.getRequesterId())
              .receiverId(connection.getReceiverId())
              .timestamp(LocalDateTime.now())
              .build();
      kafkaEventPublisher.publishEvent(connectionAcceptedTopic, event);
    }
  }

  @Override
  public void cancelRequest(Authentication authentication, UUID connectionId) {
    UUID userId =
        userService
            .getUserByKeyCloakId(UUID.fromString(authentication.getName()))
            .getBody()
            .getData()
            .getId();
    Connection connection =
        repository
            .findById(connectionId)
            .orElseThrow(() -> new RuntimeException("Connection request not found"));

    if (!connection.getRequesterId().equals(userId)) {
      throw new RuntimeException("You can only cancel your own requests");
    }

    if (connection.getStatus() != ConnectionStatus.PENDING) {
      throw new RuntimeException("Only pending requests can be cancelled");
    }

    repository.delete(connection);
  }

  @Override
  public List<ConnectionDTO> getMyConnections(Authentication authentication) {
    UUID userId =
        userService
            .getUserByKeyCloakId(UUID.fromString(authentication.getName()))
            .getBody()
            .getData()
            .getId();
    List<Connection> sent =
        repository.findByRequesterIdAndStatus(userId, ConnectionStatus.ACCEPTED);
    List<Connection> received =
        repository.findByReceiverIdAndStatus(userId, ConnectionStatus.ACCEPTED);
    List<Connection> merged = Stream.concat(sent.stream(), received.stream()).toList();
    return connectionMapper.toDto(merged);
  }

  @Override
  public List<ConnectionDTO> getPendingRequests(Authentication authentication) {
    UUID userId =
        userService
            .getUserByKeyCloakId(UUID.fromString(authentication.getName()))
            .getBody()
            .getData()
            .getId();
    List<Connection> pending =
        repository.findByReceiverIdAndStatus(userId, ConnectionStatus.PENDING);
    return connectionMapper.toDto(pending);
  }

  @Override
  public List<UUID> findMutualConnections(UUID otherUserId) {
    return java.util.Collections.emptyList();
  }

  @Override
  public UserConnectionStatusDTO getConnectionStatus(
      Authentication authentication, UUID otherUserId) {
    UUID userId =
        userService
            .getUserByKeyCloakId(UUID.fromString(authentication.getName()))
            .getBody()
            .getData()
            .getId();

    Optional<Connection> sent = repository.findByRequesterIdAndReceiverId(userId, otherUserId);
    if (sent.isPresent()) {
      return UserConnectionStatusDTO.builder()
          .connectionId(sent.get().getId())
          .status(sent.get().getStatus().name())
          .isRequester(true)
          .build();
    }

    Optional<Connection> received = repository.findByRequesterIdAndReceiverId(otherUserId, userId);
    if (received.isPresent()) {
      return UserConnectionStatusDTO.builder()
          .connectionId(received.get().getId())
          .status(received.get().getStatus().name())
          .isRequester(false)
          .build();
    }

    return UserConnectionStatusDTO.builder().status("NONE").build();
  }
}
