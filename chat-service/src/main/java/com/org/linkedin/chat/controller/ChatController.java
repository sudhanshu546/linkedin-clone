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

/**
 * Controller for managing Real-time Messaging and Presence. Supports both standard REST endpoints
 * and WebSocket message mappings for low-latency communication.
 */
@RestController
@RequestMapping("${apiPrefix}/messages")
@RequiredArgsConstructor
public class ChatController {

  private final SimpMessagingTemplate messagingTemplate;
  private final ChatService chatService;
  private final PresenceService presenceService;

  /**
   * WebSocket entry point for processing and routing private chat messages. Persists the message
   * and routes it to the recipient's private queue.
   *
   * @param chatMessageDTO The data transfer object containing message details (sender, recipient,
   *     content).
   */
  @MessageMapping("/chat")
  public void processMessage(@Payload ChatMessageDTO chatMessageDTO) {
    ChatMessageDTO saved = chatService.saveMessage(chatMessageDTO);

    messagingTemplate.convertAndSendToUser(
        saved.getRecipientId().toString(), "/queue/messages", saved);
  }

  /**
   * Routes typing indicators to the recipient via WebSockets.
   *
   * @param chatEventDTO The event DTO containing the typing status and recipient information.
   */
  @MessageMapping("/typing")
  public void processTyping(@Payload com.org.linkedin.dto.chat.ChatEventDTO chatEventDTO) {
    messagingTemplate.convertAndSendToUser(
        chatEventDTO.getRecipientId().toString(), "/queue/events", chatEventDTO);
  }

  /**
   * Processes read receipts and notifies the sender that their message was viewed.
   *
   * @param chatEventDTO The event DTO containing the message ID and sender information.
   * @param authentication The authenticated user security context.
   */
  @MessageMapping("/read-receipt")
  public void processReadReceipt(
      @Payload com.org.linkedin.dto.chat.ChatEventDTO chatEventDTO, Authentication authentication) {
    chatService.markAsRead(authentication, chatEventDTO.getSenderId());
    messagingTemplate.convertAndSendToUser(
        chatEventDTO.getSenderId().toString(), "/queue/events", chatEventDTO);
  }

  /**
   * REST endpoint to retrieve the conversation history between two users.
   *
   * @param authentication The authenticated user security context.
   * @param recipientId The unique identifier of the recipient to fetch messages with.
   * @return A ResponseEntity containing an ApiResponse with a list of ChatMessageDTOs representing
   *     the conversation.
   */
  @GetMapping("/{recipientId}")
  public ResponseEntity<ApiResponse<List<ChatMessageDTO>>> findChatMessages(
      Authentication authentication, @PathVariable UUID recipientId) {
    List<ChatMessageDTO> result = chatService.getChatMessages(authentication, recipientId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * REST endpoint to manually mark all messages from a specific sender as read.
   *
   * @param authentication The authenticated user security context.
   * @param senderId The unique identifier of the sender whose messages are being marked as read.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @PatchMapping("/read/{senderId}")
  public ResponseEntity<ApiResponse<Void>> markAsRead(
      Authentication authentication, @PathVariable UUID senderId) {
    chatService.markAsRead(authentication, senderId);
    return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
  }

  /**
   * Retrieves a set of all currently online user IDs.
   *
   * @return A ResponseEntity containing an ApiResponse with a set of UUIDs representing active
   *     sessions.
   */
  @GetMapping("/online")
  public ResponseEntity<ApiResponse<Set<UUID>>> getOnlineUsers() {
    return ResponseEntity.ok(
        ApiResponse.success("Online users fetched", presenceService.getOnlineUsers()));
  }
}
