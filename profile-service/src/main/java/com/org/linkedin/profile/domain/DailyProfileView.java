package com.org.linkedin.profile.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daily_profile_views")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyProfileView {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "profile_owner_id", nullable = false)
  private UUID profileOwnerId;

  @Column(name = "view_date", nullable = false)
  private LocalDate viewDate;

  @Column(name = "view_count")
  @Builder.Default
  private Long viewCount = 0L;
}
