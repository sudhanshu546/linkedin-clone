package com.org.linkedin.user.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.org.linkedin.user.service.EmailService;
import com.org.linkedin.utility.exception.CommonExceptionHandler;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

  private final SendGrid sendGrid;

  @Override
  public Response sendEmailDetailWithoutAttachment(
      String from, String to, String templateId, Map<String, String> dynamicData) {
    log.debug("Enter in sendEmailDetailWithoutAttachment method.");
    log.debug("Dynamic Data :: [{}]", dynamicData);
    log.debug("Mail form :: [{}]", from);
    log.debug("Mail to :: [{}]", to);
    log.debug("templateId is :: [{}]", templateId);

    Mail mail = new Mail();
    mail.setFrom(new Email(from));
    mail.setTemplateId(templateId);

    DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
    personalization.addTo(new Email(to));

    for (Map.Entry<String, String> entry : dynamicData.entrySet()) {
      personalization.addDynamicTemplateData(entry.getKey(), entry.getValue());
    }

    mail.addPersonalization(personalization);
    log.debug("Exit in sendEmailDetailWithoutAttachment method.");
    return (this.sendEmail(mail));
  }

  private Response sendEmail(Mail mail) {
    try {
      log.debug("Enter in sending mail :: mail [{}]", mail.toString());
      Request request = new Request();
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());

      Response response = sendGrid.api(request);
      log.debug("Response in sending mail :: response [{}]", response.toString());

      if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
        log.debug("Email Sent Successfully");
      } else {
        log.debug("Error in sending message :: response [{}]", response.getBody());
      }
      String status =
          (response.getStatusCode() >= 200 && response.getStatusCode() < 300)
              ? "success"
              : "failure";

      if (!status.equals("success")) {
        throw new CommonExceptionHandler(response.getBody(), HttpStatus.BAD_REQUEST.value());
      }
      log.debug("Exit in sendEmail method :: response [{}]", response);
      return response;
    } catch (IOException e) {
      log.error("Error in the sendEmail method .", e);
      throw new CommonExceptionHandler(e.getMessage(), HttpStatus.BAD_REQUEST.value());
    }
  }

  private static class DynamicTemplatePersonalization extends Personalization {

    @JsonProperty(value = "dynamic_template_data")
    private Map<String, String> dynamic_template_data;

    @JsonProperty("dynamic_template_data")
    public Map<String, String> getDynamicTemplateDataDetail() {
      log.debug("Enter in getDynamicTemplateDataDetail method.");
      if (dynamic_template_data == null) {
        log.debug("dynamic_template_data is null in getDynamicTemplateDataDetail method");
        return Collections.<String, String>emptyMap();
      }
      log.debug("Exit in getDynamicTemplateDataDetail method.");
      return dynamic_template_data;
    }

    public void addDynamicTemplateData(String key, String value) {
      log.debug("Enter in addDynamicTemplateData method :: key [{}] :: value [{}]", key, value);
      if (dynamic_template_data == null) {
        log.debug("dynamic_template_data is null in addDynamicTemplateData method");
        dynamic_template_data = new HashMap<>();
        dynamic_template_data.put(key, value);
      } else {
        dynamic_template_data.put(key, value);
        log.debug("Exit in addDynamicTemplateData method.");
      }
    }
  }
}
