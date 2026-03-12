package com.org.linkedin.chat.controller;

import com.org.linkedin.chat.repo.ChatMessageRepository;
import com.org.linkedin.domain.chat.ChatMessage;
import com.org.linkedin.utility.client.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${apiPrefix}/messages")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        
        messagingTemplate.convertAndSendToUser(
                chatMessage.getRecipientId().toString(), "/queue/messages",
                saved
        );
    }

    @GetMapping("/{recipientId}")
    public List<ChatMessage> findChatMessages(
            Authentication authentication,
            @PathVariable UUID recipientId) {
        UUID keycloakId = UUID.fromString(authentication.getName());
        UUID senderId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
        
        return chatMessageRepository.findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
                senderId, recipientId, recipientId, senderId
        );
    }
}
