package com.org.linkedin.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "users")
public class UserDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String firstName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String lastName;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String headline;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String skills;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String state;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String currentCompany;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String designation;

    @Field(type = FieldType.Keyword, index = false)
    private String profileImageUrl;
}
