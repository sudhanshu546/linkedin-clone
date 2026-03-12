package com.org.linkedin.user.service.storage;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
  String storeFile(MultipartFile file) throws IOException;

  void deleteFile(String fileUrl);
}
