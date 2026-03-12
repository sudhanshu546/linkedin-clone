package com.org.linkedin.dto.notification;

import com.org.linkedin.dto.BaseDTO;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;

@Data
public class SmsTargetDTO extends BaseDTO implements Serializable {
  private static final long serialVersionUID = 1;
  private UUID id;

  private SmsMessageDTO smsMessage;

  private UUID targetId;

  private String sendTo;
}
