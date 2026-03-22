package com.org.linkedin.chat.controller;

import com.org.linkedin.chat.service.ChatService;
import com.org.linkedin.dto.BaseResponse;
import com.org.linkedin.dto.chat.ChatMessageDTO;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

  @MessageMapping("/chat")
  public void processMessage(@Payload ChatMessageDTO chatMessageDTO) {
    ChatMessageDTO saved = chatService.saveMessage(chatMessageDTO);

    messagingTemplate.convertAndSendToUser(
        saved.getRecipientId().toString(), "/queue/messages", saved);
  }

  @GetMapping("/{recipientId}")
  public BaseResponse<List<ChatMessageDTO>> findChatMessages(
      Authentication authentication, @PathVariable UUID recipientId) {
    List<ChatMessageDTO> result = chatService.getChatMessages(authentication, recipientId);
    return BaseResponse.<List<ChatMessageDTO>>builder()
        .status(HttpStatus.OK.value())
        .result(result)
        .build();
  }

  @PatchMapping("/read/{senderId}")
  public BaseResponse<Void> markAsRead(Authentication authentication, @PathVariable UUID senderId) {
    chatService.markAsRead(authentication, senderId);
    return BaseResponse.<Void>builder()
        .status(HttpStatus.OK.value())
        .message("Messages marked as read")
        .build();
  }
}
