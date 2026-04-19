package com.org.linkedin.utility.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import net.coobird.thumbnailator.Thumbnails;
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
    Path targetPath = root.resolve(filename);

    String contentType = file.getContentType();
    // Optimize images (except GIFs to preserve animation)
    if (contentType != null && contentType.startsWith("image/") && !contentType.contains("gif")) {
      Thumbnails.of(file.getInputStream())
          .size(1200, 1200)
          .outputQuality(0.8)
          .toFile(targetPath.toFile());
    } else {
      Files.copy(file.getInputStream(), targetPath);
    }

    return filename;
  }

  @Override
  public void deleteFile(String fileUrl) {
    try {
      Path file = Paths.get(uploadDir).resolve(fileUrl);
      Files.deleteIfExists(file);
    } catch (IOException e) {
      throw new RuntimeException("Error: " + e.getMessage());
    }
  }
}
