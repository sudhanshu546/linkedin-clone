package com.org.linkedin.domain.notification;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "email_message")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage extends AbstractAuditingEntity<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String subject;

  @Column(columnDefinition = "TEXT")
  private String body;
}
