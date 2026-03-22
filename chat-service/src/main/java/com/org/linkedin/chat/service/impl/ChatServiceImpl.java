package com.org.linkedin.chat.service.impl;

import com.org.linkedin.chat.mapper.ChatMapper;
import com.org.linkedin.chat.repo.ChatMessageRepository;
import com.org.linkedin.chat.service.ChatService;
import com.org.linkedin.domain.chat.ChatMessage;
import com.org.linkedin.dto.chat.ChatMessageDTO;
import com.org.linkedin.utility.client.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

  private final ChatMessageRepository chatMessageRepository;
  private final ChatMapper chatMapper;
  private final UserService userService;

  @Override
  public ChatMessageDTO saveMessage(ChatMessageDTO chatMessageDTO) {
    ChatMessage chatMessage = chatMapper.toEntity(chatMessageDTO);
    chatMessage.setRead(false);
    chatMessage = chatMessageRepository.save(chatMessage);
    return chatMapper.toDto(chatMessage);
  }

  @Override
  public List<ChatMessageDTO> getChatMessages(Authentication authentication, UUID recipientId) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID senderId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();

    List<ChatMessage> messages =
        chatMessageRepository
            .findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByCreatedAtAsc(
                senderId, recipientId, recipientId, senderId);
    return chatMapper.toDto(messages);
  }

  @Override
  @Transactional
  public void markAsRead(Authentication authentication, UUID senderId) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID recipientId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
    chatMessageRepository.markMessagesAsRead(recipientId, senderId);
  }
}
