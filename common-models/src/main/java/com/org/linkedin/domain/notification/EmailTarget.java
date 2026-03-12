package com.org.linkedin.domain.notification;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "email_target")
@Getter
@Setter
public class EmailTarget extends AbstractAuditingEntity<UUID> {

  @NotNull
  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false)
  private UUID id;

  @ManyToOne private EmailMessage emailMessage;

  @Column(name = "target_id")
  private String targetId;
}
