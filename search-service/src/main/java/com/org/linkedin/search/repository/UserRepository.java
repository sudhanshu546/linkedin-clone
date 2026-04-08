package com.org.linkedin.search.repository;

import com.org.linkedin.search.document.UserDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface UserRepository extends ElasticsearchRepository<UserDocument, String> {
}
