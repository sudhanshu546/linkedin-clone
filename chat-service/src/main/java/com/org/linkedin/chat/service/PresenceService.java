package com.org.linkedin.chat.service;

import static com.org.linkedin.utility.ProjectConstants.DEST_PRESENCE;

import com.org.linkedin.dto.chat.ChatEventDTO;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PresenceService {

  private final SimpMessagingTemplate messagingTemplate;
  private final Map<UUID, Set<String>> userSessions = new ConcurrentHashMap<>();

  public void handleConnect(UUID userId, String sessionId) {
    userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    log.info("User {} connected. Total sessions: {}", userId, userSessions.get(userId).size());

    broadcastPresence(userId, true);
  }

  public void handleDisconnect(UUID userId, String sessionId) {
    Set<String> sessions = userSessions.get(userId);
    if (sessions != null) {
      sessions.remove(sessionId);
      if (sessions.isEmpty()) {
        userSessions.remove(userId);
        log.info("User {} disconnected (all sessions closed)", userId);
        broadcastPresence(userId, false);
      } else {
        log.info(
            "User {} disconnected session {}. Remaining sessions: {}",
            userId,
            sessionId,
            sessions.size());
      }
    }
  }

  public boolean isOnline(UUID userId) {
    return userSessions.containsKey(userId);
  }

  /** Returns a set of all user IDs currently online. */
  public Set<UUID> getOnlineUsers() {
    return userSessions.keySet();
  }

  private void broadcastPresence(UUID userId, boolean online) {
    ChatEventDTO event =
        ChatEventDTO.builder()
            .type(ChatEventDTO.EventType.PRESENCE)
            .senderId(userId)
            .online(online)
            .timestamp(System.currentTimeMillis())
            .build();

    // Broadcast to all users (simplified for now, ideally only to connections)
    messagingTemplate.convertAndSend(DEST_PRESENCE, event);
  }
}
