package com.org.linkedin.user.controller;

import com.org.linkedin.dto.BaseResponse;
import com.org.linkedin.dto.user.LoginRequest;
import com.org.linkedin.user.config.keycloak.KeycloakClients;
import com.org.linkedin.user.service.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.http.HttpStatus;
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
   * @return a {@link ResponseEntity} containing a {@link "BaseResponse"} with the {@link
   *     AccessTokenResponse}
   */
  @PostMapping("/user")
  public ResponseEntity<BaseResponse<AccessTokenResponse>> userLogin(
      @RequestBody LoginRequest loginRequest) {
    log.trace("Enter in userLogin method :: loginRequest [{}]", loginRequest);
    AccessTokenResponse accessTokenResponse =
        loginService.login(loginRequest, keycloakClient.clientName());
    log.trace("Access Token Response :: [{}]", accessTokenResponse);
    BaseResponse<AccessTokenResponse> response =
        BaseResponse.<AccessTokenResponse>builder()
            .status(HttpStatus.OK.value())
            .result(accessTokenResponse)
            .build();
    log.trace("Exit in userLogin method :: [{}]", response);
    return ResponseEntity.ok(response);
  }

  /**
   * Handles the process of obtaining a new access token for a staff member using a refresh token.
   *
   * @param refreshToken the refresh token used to generate a new access token
   * @return a {@link ResponseEntity} containing a {@link BaseResponse} with the new {@link
   *     AccessTokenResponse}
   */
  @PostMapping("user/refresh-token")
  public ResponseEntity<BaseResponse<AccessTokenResponse>> getUserTokenByRefreshToken(
      @RequestParam("refreshToken") String refreshToken) {
    log.trace("Enter in getUserTokenByRefreshToken By RefreshToken method :: [{}]", refreshToken);
    AccessTokenResponse accessTokenResponse =
        loginService.getRefreshToken(refreshToken, keycloakClient.clientName());
    log.trace("Access Token Response :: [{}]", accessTokenResponse);
    BaseResponse<AccessTokenResponse> response =
        BaseResponse.<AccessTokenResponse>builder()
            .status(HttpStatus.OK.value())
            .result(accessTokenResponse)
            .build();
    log.trace("Exit in getUserTokenByRefreshToken By RefreshToken method :: [{}]", response);
    return ResponseEntity.ok(response);
  }

  /**
   * Handles the impersonation of a user (SystemAdmin, User, or Tenant) by SystemAdmin. This
   * endpoint allows a SystemAdmin to impersonate another user by obtaining an access token for the
   * specified user (userId). The access token is generated based on the SystemAdmin's privileges,
   * allowing them to act as the target user.
   *
   * @param userId the UUID of the user or tenant to impersonate
   * @return a {@link ResponseEntity} containing an {@link AccessTokenResponse} with the token of
   *     the impersonated user
   */
  //  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  //  @PostMapping("/impLogin")
  //  public ResponseEntity<BaseResponse<AccessTokenResponse>> impersonateUser(
  //      @RequestParam UUID userId) {
  //    AccessTokenResponse response = loginService.getToken(userId, keycloakClient.clientName());
  //    BaseResponse<AccessTokenResponse> result =
  //        BaseResponse.<AccessTokenResponse>builder()
  //            .status(HttpStatus.OK.value())
  //            .result(response)
  //            .build();
  //    return ResponseEntity.ok(result);
  //  }
}
