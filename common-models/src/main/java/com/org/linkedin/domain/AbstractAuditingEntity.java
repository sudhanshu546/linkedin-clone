package com.org.linkedin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
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
public abstract class AbstractAuditingEntity<T> implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  public abstract T getId();

  @CreatedBy
  @Column(name = "created_by", updatable = false)
  private T createdBy;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private Long createdAt = System.currentTimeMillis();

  @LastModifiedBy
  @Column(name = "last_modified_by")
  private T lastModifiedBy;

  @LastModifiedDate
  @Column(name = "updated_at")
  private Long updatedAt = System.currentTimeMillis();

  @Column(name = "is_deleted", columnDefinition = "boolean default false")
  private Boolean isDeleted;

  @Column(name = "is_enabled", columnDefinition = "boolean default true")
  private Boolean isEnabled;
}
