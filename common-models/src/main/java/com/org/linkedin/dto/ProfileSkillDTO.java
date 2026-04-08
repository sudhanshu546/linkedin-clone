package com.org.linkedin.dto;

import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSkillDTO {
  private UUID id;
  private SkillDTO skill;
  private Integer endorsementCount;
}
