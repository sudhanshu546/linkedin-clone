package com.org.linkedin.user.mapper;

import com.org.linkedin.user.domain.Post;
import com.org.linkedin.user.dto.PostDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {PollOptionMapper.class})
public interface PostMapper extends EntityMapper<PostDTO, Post> {

  @Mapping(target = "authorId", source = "userId")
  @Mapping(target = "pollOptions", source = "pollOptions")
  @Mapping(target = "isPoll", source = "poll")
  PostDTO toDto(Post entity);
}
