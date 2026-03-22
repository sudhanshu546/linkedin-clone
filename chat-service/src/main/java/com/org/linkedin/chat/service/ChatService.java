package com.org.linkedin.chat.service;

import com.org.linkedin.dto.chat.ChatMessageDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface ChatService {
  ChatMessageDTO saveMessage(ChatMessageDTO chatMessageDTO);

  List<ChatMessageDTO> getChatMessages(Authentication authentication, UUID recipientId);

  void markAsRead(Authentication authentication, UUID senderId);
}
