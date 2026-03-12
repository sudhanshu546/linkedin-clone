package com.org.linkedin.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseDTO {

  String createdBy;

  Long createdDate;

  String lastModifiedBy;

  Long lastModifiedDate;

  Boolean isDeleted = false;

  Boolean isEnabled = true;
}
