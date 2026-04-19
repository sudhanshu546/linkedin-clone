package com.org.linkedin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base abstract class for entities which will hold definitions for created, last modified, created
 * by, last modified by attributes.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class AbstractAuditingEntity<T> implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  public abstract T getId();

  @CreatedBy
  @Column(name = "created_by", updatable = false)
  private T createdBy;

  @Builder.Default
  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private Long createdDate = System.currentTimeMillis();

  @LastModifiedBy
  @Column(name = "last_modified_by")
  private T lastModifiedBy;

  @Builder.Default
  @LastModifiedDate
  @Column(name = "updated_at")
  private Long lastModifiedDate = System.currentTimeMillis();

  @Builder.Default
  @Column(name = "is_deleted", columnDefinition = "boolean default false")
  private Boolean isDeleted = false;

  @Builder.Default
  @Column(name = "is_enabled", columnDefinition = "boolean default true")
  private Boolean isEnabled = true;
}
