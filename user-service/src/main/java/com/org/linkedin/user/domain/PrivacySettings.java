package com.org.linkedin.user.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "privacy_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacySettings {
  @Id private UUID userId;

  @Column(name = "profile_visibility")
  @Builder.Default
  private String profileVisibility = "PUBLIC"; // PUBLIC, CONNECTIONS, PRIVATE

  @Column(name = "show_email")
  @Builder.Default
  private boolean showEmail = true;

  @Column(name = "show_connections")
  @Builder.Default
  private boolean showConnections = true;

  @Column(name = "allow_messages_from")
  @Builder.Default
  private String allowMessagesFrom = "EVERYONE"; // EVERYONE, CONNECTIONS
}
