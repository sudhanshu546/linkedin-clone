package com.org.linkedin.dto.user;

import com.org.linkedin.constants.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;

@Data
public class ChangePassword implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID empId;
  private String emailId;

  @Size(min = 6, max = 15, message = Constants.PASSWORD_LENGTH_MESSAGE)
  @Pattern(
      regexp = "^(?!.*\\s)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d\\s])[ -~]{6,15}(?<!\\s)$",
      message = Constants.PASSWORD_CRITERIA)
  @NotBlank(message = "Password can not be empty")
  private String newPassword;

  private String confirmPassword;
}
