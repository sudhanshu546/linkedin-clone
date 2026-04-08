package com.org.linkedin.domain.notification;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "email_target")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTarget extends AbstractAuditingEntity<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String email;

  @ManyToOne
  @JoinColumn(name = "message_id")
  private EmailMessage message;
}
