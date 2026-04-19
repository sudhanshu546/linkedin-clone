package com.org.linkedin.user.mapper;

import com.org.linkedin.user.domain.Comment;
import com.org.linkedin.user.dto.CommentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper extends EntityMapper<CommentDTO, Comment> {

  @Mapping(target = "sourcePostId", source = "postId")
  @Mapping(target = "authorId", source = "userId")
  CommentDTO toDto(Comment entity);
}
