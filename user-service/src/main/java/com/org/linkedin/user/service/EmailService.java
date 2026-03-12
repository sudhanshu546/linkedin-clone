package com.org.linkedin.user.service;

import com.sendgrid.Response;
import java.util.Map;

public interface EmailService {

  Response sendEmailDetailWithoutAttachment(
      String from, String to, String templateId, Map<String, String> dynamicData);
}
