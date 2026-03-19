package com.org.linkedin.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "experience")
@Getter
@Setter
public class Experience implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "profile_id", nullable = false)
  @com.fasterxml.jackson.annotation.JsonBackReference
  private Profile profile;

  private String position;
  private String company;
  private LocalDate startDate;
  private LocalDate endDate;
  private boolean isCurrentlyWorking;

  @Column(length = 2000)
  private String description;
}
