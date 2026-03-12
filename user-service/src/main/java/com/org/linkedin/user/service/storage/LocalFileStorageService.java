package com.org.linkedin.user.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile("!prod")
public class LocalFileStorageService implements FileStorageService {

  @Value("${file.upload-dir:./uploads}")
  private String uploadDir;

  @Override
  public String storeFile(MultipartFile file) throws IOException {
    Path root = Paths.get(uploadDir);
    if (!Files.exists(root)) {
      Files.createDirectories(root);
    }
    String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
    Files.copy(file.getInputStream(), root.resolve(filename));
    return filename; // For local, we just return the filename
  }

  @Override
  public void deleteFile(String fileUrl) {
    try {
      Files.deleteIfExists(Paths.get(uploadDir).resolve(fileUrl));
    } catch (IOException e) {
      // Log error
    }
  }
}
