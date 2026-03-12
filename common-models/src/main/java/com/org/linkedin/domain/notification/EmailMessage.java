package com.org.linkedin.domain.notification;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "email_messages")
@Getter
@Setter
public class EmailMessage extends AbstractAuditingEntity<UUID> {

  @NotNull
  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false)
  private UUID id;

  private String subject;

  private String message;

  private String key;

  private UUID moduleId;
}
