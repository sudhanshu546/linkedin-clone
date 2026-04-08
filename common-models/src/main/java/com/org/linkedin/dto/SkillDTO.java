package com.org.linkedin.dto;

import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDTO {
  private UUID id;
  private String name;
  private String category;
}
