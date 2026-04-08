package com.org.linkedin.search.repository;

import com.org.linkedin.search.document.HashtagDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HashtagRepository extends ElasticsearchRepository<HashtagDocument, String> {
    List<HashtagDocument> findTop10ByOrderByCountDesc();
}
