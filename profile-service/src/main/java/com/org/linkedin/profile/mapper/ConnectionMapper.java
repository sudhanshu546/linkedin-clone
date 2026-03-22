package com.org.linkedin.profile.mapper;

import com.org.linkedin.domain.Connection;
import com.org.linkedin.dto.connection.ConnectionDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConnectionMapper extends EntityMapper<ConnectionDTO, Connection> {}
