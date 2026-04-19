package com.org.linkedin.domain;

import com.org.linkedin.domain.enumeration.ConnectionStatus;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(
    name = "connection",
    uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "receiver_id"}),
    indexes = {
      @Index(name = "idx_conn_requester_status", columnList = "requester_id, status"),
      @Index(name = "idx_conn_receiver_status", columnList = "receiver_id, status")
    })
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE connection SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false OR is_deleted IS NULL")
public class Connection extends AbstractAuditingEntity<UUID> implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "requester_id", nullable = false)
  private UUID requesterId;

  @Column(name = "receiver_id", nullable = false)
  private UUID receiverId;

  @Enumerated(EnumType.STRING)
  private ConnectionStatus status;
}
