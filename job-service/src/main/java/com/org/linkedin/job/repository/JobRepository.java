package com.org.linkedin.job.repository;

import com.org.linkedin.domain.job.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByPostedByOrderByCreatedAtDesc(UUID postedBy);

    @Query("SELECT j FROM Job j WHERE " +
           "(:query IS NULL OR (LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "(:company IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "(:description IS NULL OR LOWER(j.description) LIKE LOWER(CONCAT('%', :query, '%')))) AND " +
           "(:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:company IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :company, '%'))) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:jobType IS NULL OR j.jobType = :jobType)")
    Page<Job> searchJobs(@Param("query") String query,
                         @Param("title") String title, 
                         @Param("company") String company, 
                         @Param("location") String location, 
                         @Param("jobType") String jobType,
                         Pageable pageable);
}
