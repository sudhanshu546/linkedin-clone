package com.org.linkedin.job.mapper;

import com.org.linkedin.domain.job.Job;
import com.org.linkedin.dto.job.JobDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobMapper extends EntityMapper<JobDTO, Job> {
    @Override
    @Mapping(source = "createdDate", target = "postedDate")
    JobDTO toDto(Job entity);
}
