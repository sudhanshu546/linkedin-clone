package com.org.linkedin.dto.user;

import com.org.linkedin.constants.ErrorMessageKeys;
import com.org.linkedin.domain.user.TUser;
import com.org.linkedin.dto.BaseDTO;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/** A DTO for the {@link TUser} entity. */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TUserDTO extends BaseDTO implements Serializable {

  UUID id;

  UUID keycloakUserId;

  @Size(min = 6, max = 15, message = "Password length should be between 6 and 15 characters")
  @Pattern(
      regexp = "^(?!.*\\s)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d\\s])[ -~]{6,15}(?<!\\s)$",
      message =
          "Password must contain at least 1 uppercase letter, 1 digit, and 1 special character.Should not start or end with a space.")
  String password;

  String confirmPassword;

  @NotNull(message = ErrorMessageKeys.NAME_CANNOT_BE_BLANK)
  @Size(max = 255)
  String firstName;

  @Size(max = 255)
  String lastName;

  @NotNull(message = ErrorMessageKeys.EMAIL_CANNOT_BE_NULL)
  @Size(max = 255)
  String email;
}
