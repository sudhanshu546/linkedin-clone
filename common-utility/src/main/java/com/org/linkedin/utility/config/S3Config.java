package com.org.linkedin.utility.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3Config {

  @Value("${aws.s3.access-key:admin}")
  private String accessKey;

  @Value("${aws.s3.secret-key:password}")
  private String secretKey;

  @Value("${aws.s3.region:us-east-1}")
  private String region;

  @Value("${aws.s3.endpoint:}")
  private String endpoint;

  @Bean
  public S3Client s3Client() {
    AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
    var builder =
        S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials));

    if (endpoint != null && !endpoint.isEmpty()) {
      builder.endpointOverride(URI.create(endpoint));
      builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
    }

    return builder.build();
  }
}
