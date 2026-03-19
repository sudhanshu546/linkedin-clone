package com.org.linkedin.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileDTO {

  private UUID id;
  private UUID userId;

  @NotBlank(message = "Headline is required")
  private String headline;

  private String summary;

  private String skills;

  @NotBlank(message = "City is required")
  private String city;

  @NotBlank(message = "State is required")
  private String state;

  private Integer experienceYears;

  @NotBlank(message = "Current company is required")
  private String currentCompany;

  @NotBlank(message = "Designation is required")
  private String designation;

  private String coverImageUrl;
}
