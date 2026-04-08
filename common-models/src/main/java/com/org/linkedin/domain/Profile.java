package com.org.linkedin.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "profile", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Profile extends AbstractAuditingEntity<UUID> implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  private String headline;

  @Column(length = 2000)
  private String summary;

  private String skills;

  private String city;
  private String state;

  private Integer experienceYears;
  private String currentCompany;
  private String designation;

  @Column(name = "cover_image_url")
  private String coverImageUrl;

  @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonManagedReference
  @Builder.Default
  private List<Experience> experience = new ArrayList<>();

  @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonManagedReference
  @Builder.Default
  private List<Education> education = new ArrayList<>();

  @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonManagedReference
  @Builder.Default
  private List<ProfileSkill> skillsList = new ArrayList<>();

  @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonManagedReference
  @Builder.Default
  private List<Recommendation> recommendations = new ArrayList<>();
}
