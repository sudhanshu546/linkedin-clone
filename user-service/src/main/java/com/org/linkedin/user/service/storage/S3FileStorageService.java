package com.org.linkedin.user.service.storage;

import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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
    String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(bucketName)
            .key(fileName)
            .contentType(file.getContentType())
            .build(),
        RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

    return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
  }

  @Override
  public void deleteFile(String fileUrl) {
    String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    s3Client.deleteObject(builder -> builder.bucket(bucketName).key(fileName));
  }
}
