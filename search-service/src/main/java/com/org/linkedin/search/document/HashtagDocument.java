package com.org.linkedin.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "hashtags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashtagDocument {
    @Id
    private String tag; // the hashtag itself

    @Field(type = FieldType.Long)
    private Long count;

    @Field(type = FieldType.Long)
    private Long lastUpdated;
}
