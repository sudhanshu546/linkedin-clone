package com.org.linkedin.chat.config;

import com.org.linkedin.chat.service.PresenceService;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.utility.client.UserService;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

  private final PresenceService presenceService;
  private final UserService userService;

  @EventListener
  public void handleWebSocketConnectListener(SessionConnectedEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    Principal principal = headerAccessor.getUser();

    if (principal != null) {
      try {
        UUID keycloakId = UUID.fromString(principal.getName());
        // In a real app, we might cache this mapping or use the ID directly from the token
        TUserDTO user = userService.getUserByKeyCloakId(keycloakId).getBody().getData();
        presenceService.handleConnect(user.getId(), headerAccessor.getSessionId());
      } catch (Exception e) {
        log.error("Error handling connect event", e);
      }
    }
  }

  @EventListener
  public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    Principal principal = headerAccessor.getUser();

    if (principal != null) {
      try {
        UUID keycloakId = UUID.fromString(principal.getName());
        TUserDTO user = userService.getUserByKeyCloakId(keycloakId).getBody().getData();
        presenceService.handleDisconnect(user.getId(), headerAccessor.getSessionId());
      } catch (Exception e) {
        log.error("Error handling disconnect event", e);
      }
    }
  }
}
