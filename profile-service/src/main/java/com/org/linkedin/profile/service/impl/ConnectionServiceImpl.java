package com.org.linkedin.profile.service.impl;

import com.org.linkedin.domain.Connection;
import com.org.linkedin.domain.enumeration.ConnectionStatus;
import com.org.linkedin.dto.connection.ConnectionDTO;
import com.org.linkedin.dto.connection.UserConnectionStatusDTO;
import com.org.linkedin.dto.event.ConnectionRequestedEvent;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.profile.mapper.ConnectionMapper;
import com.org.linkedin.profile.repo.ConnectionRepository;
import com.org.linkedin.profile.service.ConnectionService;
import com.org.linkedin.utility.client.UserService;
import com.org.linkedin.utility.service.KafkaEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    UUID userId = getInternalUserId(authentication);
    List<Connection> sent =
        repository.findByRequesterIdAndStatus(userId, ConnectionStatus.ACCEPTED);
    List<Connection> received =
        repository.findByReceiverIdAndStatus(userId, ConnectionStatus.ACCEPTED);
    List<Connection> merged = Stream.concat(sent.stream(), received.stream()).toList();
    return enrichConnections(merged);
  }

  @Override
  public List<ConnectionDTO> getPendingRequests(Authentication authentication) {
    UUID userId = getInternalUserId(authentication);
    List<Connection> pending =
        repository.findByReceiverIdAndStatus(userId, ConnectionStatus.PENDING);
    return enrichConnections(pending);
  }

  private UUID getInternalUserId(Authentication authentication) {
    return userService
        .getUserByKeyCloakId(UUID.fromString(authentication.getName()))
        .getBody()
        .getData()
        .getId();
  }

  private List<ConnectionDTO> enrichConnections(List<Connection> connections) {
    if (connections.isEmpty()) return java.util.Collections.emptyList();

    Set<UUID> userIds = new java.util.HashSet<>();
    connections.forEach(
        c -> {
          userIds.add(c.getRequesterId());
          userIds.add(c.getReceiverId());
        });

    try {
      var userRes = userService.getUsersByIds(new java.util.ArrayList<>(userIds));
      if (userRes != null && userRes.getBody() != null && userRes.getBody().getData() != null) {
        var users = userRes.getBody().getData();
        var userMap =
            users.stream().collect(java.util.stream.Collectors.toMap(TUserDTO::getId, u -> u));

        return connections.stream()
            .map(
                c -> {
                  ConnectionDTO dto = connectionMapper.toDto(c);

                  TUserDTO requester = userMap.get(c.getRequesterId());
                  if (requester != null) {
                    dto.setRequesterName(requester.getFirstName() + " " + requester.getLastName());
                    dto.setRequesterAvatar(requester.getProfileImageUrl());
                    //            dto.setRequesterDesignation(requester.getDesignation());
                  }

                  TUserDTO receiver = userMap.get(c.getReceiverId());
                  if (receiver != null) {
                    dto.setReceiverName(receiver.getFirstName() + " " + receiver.getLastName());
                    dto.setReceiverAvatar(receiver.getProfileImageUrl());
                    //            dto.setReceiverDesignation(receiver.getDesignation());
                  }

                  return dto;
                })
            .collect(java.util.stream.Collectors.toList());
      }
    } catch (Exception e) {
      log.error("Failed to enrich connections: {}", e.getMessage());
    }

    return connectionMapper.toDto(connections);
  }

  @Override
  public List<UUID> findMutualConnections(UUID otherUserId) {
    // This usually requires the current user context
    // For now we'll implement the intersection logic
    // We would need current user ID here, but the interface only has otherUserId.
    // Let's assume this is called from a context where we can get the current user,
    // or we might need to change the signature.
    return java.util.Collections.emptyList();
  }

  private Set<UUID> getFriendsIds(UUID userId) {
    List<Connection> sent =
        repository.findByRequesterIdAndStatus(userId, ConnectionStatus.ACCEPTED);
    List<Connection> received =
        repository.findByReceiverIdAndStatus(userId, ConnectionStatus.ACCEPTED);

    Set<UUID> friendIds = new java.util.HashSet<>();
    sent.forEach(c -> friendIds.add(c.getReceiverId()));
    received.forEach(c -> friendIds.add(c.getRequesterId()));
    return friendIds;
  }

  @Override
  public List<UUID> findMutualConnections(Authentication authentication, UUID otherUserId) {
    UUID myId = getInternalUserId(authentication);

    Set<UUID> myFriends = getFriendsIds(myId);
    Set<UUID> otherFriends = getFriendsIds(otherUserId);

    // Intersection
    myFriends.retainAll(otherFriends);
    return new java.util.ArrayList<>(myFriends);
  }

  @Override
  public List<com.org.linkedin.dto.user.TUserDTO> getNetworkSuggestions(
      Authentication authentication) {
    UUID userId = getInternalUserId(authentication);

    // 1. Get friend-of-friends (2nd degree) from native query
    List<UUID> suggestionIds = repository.findPeopleYouMayKnow(userId);

    // 2. If we don't have enough suggestions, get some random active users
    if (suggestionIds.size() < 10) {
      // Logic for random users who are not already connected or pending
      // For now we'll just use the suggested IDs
    }

    if (suggestionIds.isEmpty()) return java.util.Collections.emptyList();

    try {
      var userRes = userService.getUsersByIds(suggestionIds);
      if (userRes != null && userRes.getBody() != null) {
        return userRes.getBody().getData();
      }
    } catch (Exception e) {
      log.error("Failed to fetch suggestion details: {}", e.getMessage());
    }

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
