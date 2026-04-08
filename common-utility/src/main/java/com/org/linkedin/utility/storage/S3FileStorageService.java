package com.org.linkedin.utility.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Profile("prod | s3")
public class S3FileStorageService implements FileStorageService {

  private final S3Client s3Client;

  @Value("${aws.s3.bucket:linkedin-uploads}")
  private String bucketName;

  @Value("${aws.s3.region:us-east-1}")
  private String region;

  @Value("${aws.s3.public-url:}")
  private String publicUrl;

  public S3FileStorageService(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  @PostConstruct
  public void init() {
    try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
    } catch (NoSuchBucketException e) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    }
  }

  @Override
  public String storeFile(MultipartFile file) throws IOException {
    String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(bucketName)
            .key(filename)
            .contentType(file.getContentType())
            .build();

    s3Client.putObject(
        putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

    if (publicUrl != null && !publicUrl.isEmpty()) {
      return String.format("%s/%s/%s", publicUrl, bucketName, filename);
    }
    return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, filename);
  }

  @Override
  public void deleteFile(String fileUrl) {
    try {
      String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder().bucket(bucketName).key(filename).build();
      s3Client.deleteObject(deleteObjectRequest);
    } catch (Exception e) {
      throw new RuntimeException("Error: " + e.getMessage());
    }
  }
}
