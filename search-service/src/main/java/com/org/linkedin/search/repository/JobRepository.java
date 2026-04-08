package com.org.linkedin.search.repository;

import com.org.linkedin.search.document.JobDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface JobRepository extends ElasticsearchRepository<JobDocument, String> {
}
