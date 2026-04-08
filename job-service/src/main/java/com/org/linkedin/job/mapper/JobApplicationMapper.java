package com.org.linkedin.job.mapper;

import com.org.linkedin.domain.job.JobApplication;
import com.org.linkedin.dto.job.JobApplicationDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface JobApplicationMapper extends EntityMapper<JobApplicationDTO, JobApplication> {}
