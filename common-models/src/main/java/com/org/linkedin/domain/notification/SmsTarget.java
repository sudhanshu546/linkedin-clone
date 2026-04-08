package com.org.linkedin.domain.notification;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sms_target")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SmsTarget extends AbstractAuditingEntity<UUID> {
  @NotNull
  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false)
  private UUID id;

  @ManyToOne private SmsMessage smsMessage;

  @Column(name = "target_id")
  private UUID targetId;
}
