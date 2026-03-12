package com.org.linkedin.user.config;

import com.org.linkedin.user.service.impl.EmailServiceImpl;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@Data
public class AsyncCalls {

  @Value("${sendgrid.emailConfig.senderEmail}")
  private String senderEmail;

  @Value("${sendgrid.templateId.registration}")
  private String registrationTemplateId;

  @Value("${sendgrid.templateId.forgotPassword}")
  private String forgotPasswordTemplateId;

  private final EmailServiceImpl emailService;

  @Async
  public void sendRegistrationMail(String firstName, String email, String password) {
    Map<String, String> activationMap = new HashMap<>();
    activationMap.put("firstName", firstName);
    activationMap.put("email", email);
    activationMap.put("password", password);

    emailService.sendEmailDetailWithoutAttachment(
        senderEmail, email, registrationTemplateId, activationMap);
  }

  public void sendForgotLink(String firstName, String email, String url) {
    Map<String, String> activationMap = new HashMap<>();
    activationMap.put("firstName", firstName);
    activationMap.put("resetPasswordLink", url);
    emailService.sendEmailDetailWithoutAttachment(
        senderEmail, email, forgotPasswordTemplateId, activationMap);
  }
}
