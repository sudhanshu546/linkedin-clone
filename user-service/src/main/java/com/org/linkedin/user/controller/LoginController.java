package com.org.linkedin.user.controller;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.user.LoginRequest;
import com.org.linkedin.user.config.keycloak.KeycloakClients;
import com.org.linkedin.user.service.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${apiPrefix}/login")
@Slf4j
public class LoginController {

  private final LoginService loginService;
  private final KeycloakClients keycloakClient;

  /**
   * Handles the staff login process by authenticating the staff member and generating an access
   * token.
   *
   * @param loginRequest the login credentials of the staff member
   * @return a {@link ResponseEntity} containing a {@link ApiResponse} with the {@link
   *     AccessTokenResponse}
   */
  @PostMapping("/user")
  public ResponseEntity<ApiResponse<AccessTokenResponse>> userLogin(
      @RequestBody LoginRequest loginRequest) {
    log.debug("Enter in userLogin method :: loginRequest [{}]", loginRequest);
    AccessTokenResponse accessTokenResponse =
        loginService.login(loginRequest, keycloakClient.clientName());
    log.debug("Access Token Response :: [{}]", accessTokenResponse);
    return ResponseEntity.ok(ApiResponse.success("Login successful", accessTokenResponse));
  }

  /**
   * Handles the process of obtaining a new access token for a staff member using a refresh token.
   *
   * @param refreshToken the refresh token used to generate a new access token
   * @return a {@link ResponseEntity} containing a {@link ApiResponse} with the new {@link
   *     AccessTokenResponse}
   */
  @PostMapping("user/refresh-token")
  public ResponseEntity<ApiResponse<AccessTokenResponse>> getUserTokenByRefreshToken(
      @RequestParam("refreshToken") String refreshToken) {
    log.debug("Enter in getUserTokenByRefreshToken By RefreshToken method :: [{}]", refreshToken);
    AccessTokenResponse accessTokenResponse =
        loginService.getRefreshToken(refreshToken, keycloakClient.clientName());
    log.debug("Access Token Response :: [{}]", accessTokenResponse);
    return ResponseEntity.ok(
        ApiResponse.success("Token refreshed successfully", accessTokenResponse));
  }
}
