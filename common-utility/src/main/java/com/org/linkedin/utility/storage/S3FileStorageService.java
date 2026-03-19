package com.org.linkedin.utility.storage;

import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Service
@Profile("prod")
public class S3FileStorageService implements FileStorageService {

  private final S3Client s3Client;

  @Value("${aws.s3.bucket}")
  private String bucketName;

  @Value("${aws.s3.region}")
  private String region;

  public S3FileStorageService(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  @Override
  public String storeFile(MultipartFile file) throws IOException {
    String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder().bucket(bucketName).key(filename).build();
    s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
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
