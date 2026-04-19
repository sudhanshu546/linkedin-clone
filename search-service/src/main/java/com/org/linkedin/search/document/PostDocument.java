package com.org.linkedin.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "posts")
public class PostDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String authorId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String authorName;

    @Field(type = FieldType.Keyword, index = false)
    private String userProfileImageUrl;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Keyword, index = false)
    private String imageUrl;

    @Field(type = FieldType.Keyword, index = false)
    private List<String> imageUrls;

    @Field(type = FieldType.Boolean)
    private boolean isPoll;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String pollQuestion;

    @Field(type = FieldType.Keyword)
    private List<String> pollOptions;
}
