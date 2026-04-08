package com.org.linkedin.user.controller;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.user.service.EmailService;
import com.sendgrid.Response;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${apiPrefix}/mail")
@AllArgsConstructor
public class MailController {

  private final EmailService mailService;

  /**
   * Sends an email using SendGrid without attachments.
   *
   * @param emailRequest A map containing: - "from": Sender's email. - "to": Recipient's email. -
   *     "templateId": SendGrid template ID. - "dynamicData": Key-value pairs for template data.
   * @return ResponseEntity with the response body from the email service.
   */
  @Operation(summary = "This Api is use to send Email")
  @PostMapping("/sendEmail")
  public ResponseEntity<ApiResponse<String>> sendEmailWithoutAttachment(
      @RequestBody Map<String, Object> emailRequest) {
    log.debug("Enter in sendEmailWithoutAttachment method :: emailRequest [{}]", emailRequest);
    Response response =
        mailService.sendEmailDetailWithoutAttachment(
            (String) emailRequest.get("from"),
            (String) emailRequest.get("to"),
            (String) emailRequest.get("templateId"),
            (Map<String, String>) emailRequest.get("dynamicData"));
    log.debug("Exit in sendEmailWithoutAttachment method :: response [{}]", response.getBody());
    return ResponseEntity.ok(ApiResponse.success("Email sent successfully", response.getBody()));
  }
}
