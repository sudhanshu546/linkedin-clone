package com.org.linkedin.dto.notification;

import com.org.linkedin.dto.BaseDTO;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;

@Data
public class NotificationSentToDTO extends BaseDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID id;

  private UUID targetId;

  private Integer readStatus;

  private Long readTime;
}
