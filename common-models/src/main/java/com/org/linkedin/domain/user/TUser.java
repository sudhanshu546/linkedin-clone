package com.org.linkedin.domain.user;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** A TUser. */
@Entity
@Table(name = "t_user")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TUser extends AbstractAuditingEntity<UUID> implements Serializable {

  @NotNull
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  UUID id;

  @Column(name = "keycloak_user_id", unique = true)
  UUID keycloakUserId;

  @NotNull
  @Size(max = 255)
  @Column(name = "email", length = 255, nullable = false)
  String email;

  String password;

  @NotNull
  @Size(max = 255)
  @Column(name = "first_name", length = 255, nullable = false)
  String firstName;

  @Size(max = 255)
  @Column(name = "last_name", length = 255)
  String lastName;

  @Column(name = "profile_image_url")
  String profileImageUrl;

  boolean enabled;
}
