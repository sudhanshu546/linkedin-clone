package com.org.linkedin.job.mapper;

import com.org.linkedin.domain.job.JobApplication;
import com.org.linkedin.dto.job.JobApplicationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobApplicationMapper extends EntityMapper<JobApplicationDTO, JobApplication> {
    @Override
    @Mapping(source = "createdDate", target = "appliedDate")
    JobApplicationDTO toDto(JobApplication entity);
}
