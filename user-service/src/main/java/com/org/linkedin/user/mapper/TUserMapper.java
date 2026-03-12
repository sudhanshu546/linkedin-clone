package com.org.linkedin.user.mapper;

import com.org.linkedin.domain.user.TUser;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.user.dto.CommonListDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TUserMapper extends EntityMapper<TUserDTO, TUser> {
  @Mapping(source = "keycloakUserId", target = "id")
  @Mapping(target = "name", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
  CommonListDto toListDto(TUser user);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "keycloakUserId", ignore = true)
  @Mapping(target = "email", ignore = true)
  void toExistingUser(@MappingTarget TUser user, TUserDTO userDTO);
}
