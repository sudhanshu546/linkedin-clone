package com.org.linkedin.domain.notification;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "notification_sent_to")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(
    callSuper = false,
    exclude = {"notificationTarget"})
@ToString(exclude = {"notificationTarget"})
public class NotificationSentTo extends AbstractAuditingEntity<UUID> implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false)
  private UUID id;

  @ManyToOne private NotificationTarget notificationTarget;

  @Column(name = "target_id")
  private UUID targetId;

  @Column(name = "read_status")
  private Integer readStatus;

  @Column(name = "read_time")
  private Long readTime;
}
