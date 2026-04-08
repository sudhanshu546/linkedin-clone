package com.org.linkedin.profile.mapper;

import com.org.linkedin.domain.Profile;
import com.org.linkedin.domain.ProfileSkill;
import com.org.linkedin.domain.Recommendation;
import com.org.linkedin.dto.ProfileDTO;
import com.org.linkedin.dto.ProfileSkillDTO;
import com.org.linkedin.dto.RecommendationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper extends EntityMapper<ProfileDTO, Profile> {

  @Mapping(source = "skillsList", target = "skillsList")
  @Mapping(source = "recommendations", target = "recommendations")
  ProfileDTO toDto(Profile entity);

  ProfileSkillDTO toDto(ProfileSkill entity);

  @Mapping(target = "authorName", ignore = true)
  @Mapping(target = "authorHeadline", ignore = true)
  RecommendationDTO toDto(Recommendation entity);
}
