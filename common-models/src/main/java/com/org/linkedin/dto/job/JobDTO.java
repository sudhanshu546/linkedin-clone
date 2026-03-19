package com.org.linkedin.dto.job;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDTO {
  private UUID id;
  private String title;
  private String description;
  private String company;
  private String location;
  private String jobType;
  private UUID postedBy;
  private Long postedDate;
}
