package com.org.linkedin.chat.mapper;

import com.org.linkedin.domain.chat.ChatMessage;
import com.org.linkedin.dto.chat.ChatMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMapper extends EntityMapper<ChatMessageDTO, ChatMessage> {
    @Override
    @Mapping(source = "createdDate", target = "timestamp")
    ChatMessageDTO toDto(ChatMessage entity);
}
