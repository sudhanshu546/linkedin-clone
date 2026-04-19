package com.org.linkedin.search.service;

import com.org.linkedin.search.document.JobDocument;
import com.org.linkedin.search.document.PostDocument;
import com.org.linkedin.search.document.UserDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final com.org.linkedin.search.repository.HashtagRepository hashtagRepository;

    public List<String> getTrendingHashtags() {
        return hashtagRepository.findTop10ByOrderByCountDesc().stream()
                .map(com.org.linkedin.search.document.HashtagDocument::getTag)
                .collect(Collectors.toList());
    }

    public List<UserDocument> searchUsers(String queryText, int page, int size) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                    .multiMatch(m -> m
                        .fields("firstName", "lastName", "headline", "skills", "currentCompany", "designation")
                        .query(queryText)
                        .fuzziness("AUTO")
                    )
                )
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<UserDocument> searchHits = elasticsearchOperations.search(query, UserDocument.class);
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    public List<JobDocument> searchJobs(String queryText, int page, int size) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                    .multiMatch(m -> m
                        .fields("title", "description", "company", "location")
                        .query(queryText)
                        .fuzziness("AUTO")
                    )
                )
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<JobDocument> searchHits = elasticsearchOperations.search(query, JobDocument.class);
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    public List<PostDocument> searchPosts(String queryText, int page, int size) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                    .multiMatch(m -> m
                        .fields("content", "authorName", "pollQuestion")
                        .query(queryText)
                        .fuzziness("AUTO")
                    )
                )
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<PostDocument> searchHits = elasticsearchOperations.search(query, PostDocument.class);
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
}
