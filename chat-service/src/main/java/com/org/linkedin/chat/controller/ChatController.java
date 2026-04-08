package com.org.linkedin.chat.controller;

import com.org.linkedin.chat.service.ChatService;
import com.org.linkedin.chat.service.PresenceService;
import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.chat.ChatMessageDTO;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${apiPrefix}/messages")
@RequiredArgsConstructor
public class ChatController {

  private final SimpMessagingTemplate messagingTemplate;
  private final ChatService chatService;
  private final PresenceService presenceService;

  @MessageMapping("/chat")
  public void processMessage(@Payload ChatMessageDTO chatMessageDTO) {
    ChatMessageDTO saved = chatService.saveMessage(chatMessageDTO);

    messagingTemplate.convertAndSendToUser(
        saved.getRecipientId().toString(), "/queue/messages", saved);
  }

  @MessageMapping("/typing")
  public void processTyping(@Payload com.org.linkedin.dto.chat.ChatEventDTO chatEventDTO) {
    messagingTemplate.convertAndSendToUser(
        chatEventDTO.getRecipientId().toString(), "/queue/events", chatEventDTO);
  }

  @MessageMapping("/read-receipt")
  public void processReadReceipt(
      @Payload com.org.linkedin.dto.chat.ChatEventDTO chatEventDTO, Authentication authentication) {
    chatService.markAsRead(authentication, chatEventDTO.getSenderId());
    messagingTemplate.convertAndSendToUser(
        chatEventDTO.getSenderId().toString(), "/queue/events", chatEventDTO);
  }

  @GetMapping("/{recipientId}")
  public ResponseEntity<ApiResponse<List<ChatMessageDTO>>> findChatMessages(
      Authentication authentication, @PathVariable UUID recipientId) {
    List<ChatMessageDTO> result = chatService.getChatMessages(authentication, recipientId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @PatchMapping("/read/{senderId}")
  public ResponseEntity<ApiResponse<Void>> markAsRead(
      Authentication authentication, @PathVariable UUID senderId) {
    chatService.markAsRead(authentication, senderId);
    return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
  }

  /**
   * Returns a list of all currently online user IDs.
   */
  @GetMapping("/online")
  public ResponseEntity<ApiResponse<Set<UUID>>> getOnlineUsers() {
    return ResponseEntity.ok(ApiResponse.success("Online users fetched", presenceService.getOnlineUsers()));
  }
}
