package com.org.linkedin.user.dto;

import com.org.linkedin.constants.ErrorMessageKeys;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.UUID;

public class CommonListDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID id;

  private String name;

  @NotBlank(message = ErrorMessageKeys.CODE_CANNOT_BE_BLANK)
  private String code;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
