package com.org.linkedin.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "skills")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Skill extends AbstractAuditingEntity<UUID> implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  private String category;
}
