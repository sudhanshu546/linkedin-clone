package com.org.linkedin.dto.user;

import com.org.linkedin.constants.ErrorMessageKeys;
import com.org.linkedin.dto.BaseDTO;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleDTO extends BaseDTO implements Serializable {

  static final long serialVersionUID = 1L;

  UUID id;

  @NotBlank(message = ErrorMessageKeys.NAME_CANNOT_BE_BLANK)
  String name;

  @NotBlank(message = ErrorMessageKeys.CODE_CANNOT_BE_BLANK)
  String code;

  String description;

  String keyCloakRoleId;
}
