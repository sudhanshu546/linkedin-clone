package com.org.linkedin.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacySettingsDTO {
  private String profileVisibility; // PUBLIC, CONNECTIONS, PRIVATE
  private boolean showEmail;
  private boolean showConnections;
  private String allowMessagesFrom; // EVERYONE, CONNECTIONS
}
