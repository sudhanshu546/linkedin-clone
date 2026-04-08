package com.org.linkedin.domain.notification;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sms_messages")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SmsMessage extends AbstractAuditingEntity<UUID> {

  @NotNull
  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "subject")
  private String subject;

  @Column(name = "message")
  private String message;

  @Column(name = "key")
  private String key;

  @Column(name = "module_id")
  private UUID moduleId;
}
