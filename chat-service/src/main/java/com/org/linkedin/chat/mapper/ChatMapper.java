package com.org.linkedin.chat.mapper;

import com.org.linkedin.domain.chat.ChatMessage;
import com.org.linkedin.dto.chat.ChatMessageDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatMapper extends EntityMapper<ChatMessageDTO, ChatMessage> {}
