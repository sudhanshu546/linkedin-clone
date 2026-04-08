package com.org.linkedin.domain.notification;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "notification_target")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(
    callSuper = false,
    exclude = {"notificationSentTo"})
@ToString(exclude = {"notificationSentTo"})
public class NotificationTarget extends AbstractAuditingEntity<UUID> implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @NotNull
  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  private Notification notification;

  @Column(name = "target_id")
  private UUID targetId;

  @Column(name = "read_status")
  private Integer readStatus;

  @Column(name = "read_time")
  private Long readTime;

  @Column(name = "sent_from")
  private UUID sentFrom;

  @Column(name = "sent_time")
  private Long sentTime;

  @Column(name = "custom_message")
  private String customMessage;
}
