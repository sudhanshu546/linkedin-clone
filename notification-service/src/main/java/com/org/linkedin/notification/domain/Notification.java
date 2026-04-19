package com.org.linkedin.notification.domain;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(
    name = "notification",
    indexes = {@Index(name = "idx_notification_recipient_read", columnList = "recipient_id, is_read, created_at DESC")})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE notification SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false OR is_deleted IS NULL")
public class Notification extends AbstractAuditingEntity<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "recipient_id", nullable = false)
  private UUID recipientId;

  @Column(name = "sender_id")
  private UUID senderId;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "message", nullable = false)
  private String message;

  @Column(name = "is_read")
  @Builder.Default
  private boolean isRead = false;
}
