package com.org.linkedin.dto.notification;

import com.org.linkedin.dto.BaseDTO;
import java.io.Serializable;
import java.util.*;
import lombok.Data;

@Data
public class NotificationTargetDTO extends BaseDTO implements Serializable {
  private static final long serialVersionUID = 1L;

  private UUID id;

  private NotificationDTO notification;

  private UUID sentFrom;

  private Long sentTime;

  private String customMessage;

  private String key;

  private List<String> customKeys = new ArrayList<>();

  private UUID tenantId;

  private UUID roleId;

  //  private NotificationSendBy sendBy;

  private UUID targetId;

  private Integer readStatus = 0;

  private Long readTime;
}
