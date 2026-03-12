package com.org.linkedin.dto.notification;

import com.org.linkedin.dto.BaseDTO;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;

@Data
public class NotificationDTO extends BaseDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID id;

  private UUID recipientId;

  private String notification;

  private String heading;

  private String url;

  private String key;

  private Integer type;

  private String notificationIcon;

  private Integer status;

  private Integer isArchive;
}
