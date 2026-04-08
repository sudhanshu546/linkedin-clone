package com.org.linkedin.dto.event;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatedEvent {
  private UUID id;
  private String firstName;
  private String lastName;
  private String email;
  private String headline;
  private String skills;
  private String city;
  private String state;
  private String currentCompany;
  private String designation;
  private String profileImageUrl;
  private Long updatedAt;
  private String action; // CREATE, UPDATE, DELETE
}
