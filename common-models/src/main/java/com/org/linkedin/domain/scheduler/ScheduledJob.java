// package com.org.linkedin.domain.scheduler;
//
// import jakarta.persistence.Access;
// import jakarta.persistence.AccessType;
// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.EnumType;
// import jakarta.persistence.Enumerated;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.Id;
// import jakarta.persistence.Table;
// import java.io.Serializable;
// import java.time.Instant;
// import java.util.UUID;
// import lombok.AllArgsConstructor;
// import lombok.EqualsAndHashCode;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;
// import lombok.ToString;
//
// @Entity
// @Table(name = "scheduled_job")
//// @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
// @Access(AccessType.FIELD)
// @Getter
// @Setter
// @EqualsAndHashCode(callSuper = false)
// @ToString
// @AllArgsConstructor
// @NoArgsConstructor
// public class ScheduledJob implements Serializable {
//
//  /** */
//  private static final long serialVersionUID = -4377115542452561353L;
//
//  @Id
//  @GeneratedValue
//  @Column(name = "id", nullable = false)
//  private UUID id;
//
//  @Enumerated(EnumType.STRING)
//  @Column(name = "job_type")
//  private JobType jobType;
//
//  @Column(name = "start_time")
//  private Instant startTime;
//
//  @Column(name = "sync_from")
//  private Instant syncFrom;
//
//  @Column(name = "completion_time")
//  private Instant completionTime;
//
//  @Column(name = "total_records")
//  private Integer totalRecords;
//
//  @Column(name = "synced_records")
//  private Integer syncedRecords;
//
//  @Column(name = "failed_records")
//  private Integer failedRecords;
//
//  @Column(name = "error_message", columnDefinition = "TEXT")
//  private String errorMessage;
//
//  @Column(name = "failed_list", columnDefinition = "TEXT")
//  private String failedList;
//
//  @Enumerated(EnumType.STRING)
//  @Column(name = "status")
//  private JobStatus status;
//
//  @Column(name = "duration")
//  private String duration;
//
//  @Column(name = "retry_count")
//  private Integer retryCount = 0;
//
//  @Column(name = "is_retried", nullable = false, columnDefinition = "boolean  default  false")
//  private boolean isRetried;
//
//  public ScheduledJob(JobType jobType, Instant startTime, Instant syncFrom) {
//    super();
//    this.jobType = jobType;
//    this.startTime = startTime;
//    this.status = JobStatus.PENDING;
//    this.syncFrom = syncFrom;
//  }
// }
