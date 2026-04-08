package com.org.linkedin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {
  private static final long serialVersionUID = 1L;

  private String status; // success, fail, error
  private String message;
  private T data;
  private Map<String, Object> metadata;

  public static <T> ApiResponse<T> success(String message, T data) {
    return ApiResponse.<T>builder().status("success").message(message).data(data).build();
  }

  public static <T> ApiResponse<T> success(String message, T data, int page, int size, long total) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("page", page);
    metadata.put("size", size);
    metadata.put("total", total);
    return ApiResponse.<T>builder()
        .status("success")
        .message(message)
        .data(data)
        .metadata(metadata)
        .build();
  }

  public static <T> ApiResponse<T> fail(String message) {
    return ApiResponse.<T>builder().status("fail").message(message).build();
  }

  public static <T> ApiResponse<T> error(String message) {
    return ApiResponse.<T>builder().status("error").message(message).build();
  }
}
