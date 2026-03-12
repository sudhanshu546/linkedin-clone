package com.org.linkedin.user.config;

import com.org.linkedin.domain.auditLog.AuditLog;
import com.org.linkedin.user.repository.auditLog.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

  private final AuditLogRepository logRepository;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    try {
      String responseBody = (String) request.getAttribute("responseBody");
      String userId = getAuthenticatedUserId();
      String status = response.getStatus() == 200 ? "SUCCESS" : "FAILURE";
      Boolean isError = response.getStatus() != 200;
      String failureReason = ex != null ? ex.getMessage() : "None";
      String ipAddress = request.getServerName();

      // Save the log information in the database.
      saveLogToDatabase(
          request.getRequestURI(), request.getMethod(), status, isError, ipAddress, userId);
    } catch (Exception e) {
      log.error("Failed to log response", e);
    }
  }

  private void saveLogToDatabase(
      String uri, String method, String status, Boolean isError, String ipAddress, String userId) {
    AuditLog auditLog = new AuditLog();
    auditLog.setMethod(method);
    auditLog.setStatus(status);
    auditLog.setDate(LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli());
    auditLog.setEndPoint(uri);
    auditLog.setIsError(isError);
    auditLog.setIsDeleted(false);
    auditLog.setIsEnabled(true);
    auditLog.setIpAddress(ipAddress);
    if (userId != null) {
      auditLog.setUserId(UUID.fromString(userId));
    }
    logRepository.save(auditLog);
  }

  private String getAuthenticatedUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      String userId = authentication.getName();
      return "anonymousUser".equals(userId) ? null : userId;
    }
    return null;
  }
}
