package com.org.linkedin.dto.job;

import com.org.linkedin.dto.user.TUserDTO;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationDTO {
  private UUID id;
  private UUID jobId;
  private UUID applicantId;
  private String status;
  private Long appliedDate;
  private TUserDTO applicant;
  private String resumeUrl;
  private String coverLetter;
}
