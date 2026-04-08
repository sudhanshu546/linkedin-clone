package com.org.linkedin.domain.notification;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notification")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends AbstractAuditingEntity<UUID> {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "recipient_id", nullable = false)
  private UUID recipientId;

  @Size(max = 2000)
  @Column(name = "notification", length = 2000)
  private String notification;

  @Size(max = 255)
  @Column(name = "heading", length = 255)
  private String heading;

  @Size(max = 255)
  @Column(name = "url", length = 255)
  private String url;

  @Size(max = 255)
  @Column(name = "key", length = 255)
  private String key;

  private Integer type;

  @Size(max = 255)
  @Column(name = "notification_icon", length = 255)
  private String notificationIcon;

  @Column(name = "status")
  private Integer status;

  @Column(name = "is_archive")
  private Integer isArchive;
}
