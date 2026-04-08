package com.org.linkedin.utility.exception;

public class ResourceNotFoundException extends BusinessException {
  public ResourceNotFoundException(String message) {
    super(message);
  }

  @Override
  public int getStatusCode() {
    return 404;
  }
}
