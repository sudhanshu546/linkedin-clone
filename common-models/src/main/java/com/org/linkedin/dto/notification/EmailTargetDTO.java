package com.org.linkedin.dto.notification;

import com.org.linkedin.dto.BaseDTO;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;

@Data
public class EmailTargetDTO extends BaseDTO implements Serializable {
  private static final long serialVersionUID = 1;
  private UUID id;

  private EmailMessageDTO emailMessage;

  private UUID targetId;

  private String targetEmail;
}
