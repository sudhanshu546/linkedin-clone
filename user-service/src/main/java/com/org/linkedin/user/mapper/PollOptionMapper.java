package com.org.linkedin.user.mapper;

import com.org.linkedin.dto.poll.PollOptionDTO;
import com.org.linkedin.user.domain.PollOption;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PollOptionMapper extends EntityMapper<PollOptionDTO, PollOption> {}
