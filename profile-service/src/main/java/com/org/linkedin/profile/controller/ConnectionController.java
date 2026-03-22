package com.org.linkedin.profile.controller;

import com.org.linkedin.dto.BaseResponse;
import com.org.linkedin.dto.connection.ConnectionDTO;
import com.org.linkedin.dto.connection.ConnectionRequestDTO;
import com.org.linkedin.dto.connection.UserConnectionStatusDTO;
import com.org.linkedin.profile.service.ConnectionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${apiPrefix}/connections")
@RequiredArgsConstructor
public class ConnectionController {

  private final ConnectionService connectionService;

  @PostMapping("/request")
  public BaseResponse<Void> sendRequest(
      Authentication authentication, @RequestBody ConnectionRequestDTO dto) {
    connectionService.sendRequest(authentication, dto.getReceiverId());
    return BaseResponse.<Void>builder()
        .status(HttpStatus.OK.value())
        .message("Connection request sent")
        .build();
  }

  @PostMapping("/{id}/respond")
  public BaseResponse<Void> respond(
      Authentication authentication,
      @PathVariable UUID id,
      @RequestParam("accept") boolean accept) {
    connectionService.respondToRequest(authentication, id, accept);
    return BaseResponse.<Void>builder()
        .status(HttpStatus.OK.value())
        .message(accept ? "Connection accepted" : "Connection rejected")
        .build();
  }

  @DeleteMapping("/{id}/cancel")
  public BaseResponse<Void> cancelRequest(Authentication authentication, @PathVariable UUID id) {
    connectionService.cancelRequest(authentication, id);
    return BaseResponse.<Void>builder()
        .status(HttpStatus.OK.value())
        .message("Connection request cancelled")
        .build();
  }

  @GetMapping
  public BaseResponse<List<ConnectionDTO>> myConnections(Authentication authentication) {
    List<ConnectionDTO> result = connectionService.getMyConnections(authentication);
    return BaseResponse.<List<ConnectionDTO>>builder()
        .status(HttpStatus.OK.value())
        .result(result)
        .build();
  }

  @GetMapping("/pending")
  public BaseResponse<List<ConnectionDTO>> getPending(Authentication authentication) {
    List<ConnectionDTO> result = connectionService.getPendingRequests(authentication);
    return BaseResponse.<List<ConnectionDTO>>builder()
        .status(HttpStatus.OK.value())
        .result(result)
        .build();
  }

  @GetMapping("/status/{otherUserId}")
  public BaseResponse<UserConnectionStatusDTO> getStatus(
      Authentication authentication, @PathVariable UUID otherUserId) {
    UserConnectionStatusDTO result =
        connectionService.getConnectionStatus(authentication, otherUserId);
    return BaseResponse.<UserConnectionStatusDTO>builder()
        .status(HttpStatus.OK.value())
        .result(result)
        .build();
  }

  @GetMapping("/recommendations")
  public BaseResponse<List<UUID>> getRecommendations(Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());
    List<UUID> result = connectionService.findMutualConnections(userId);
    return BaseResponse.<List<UUID>>builder().status(HttpStatus.OK.value()).result(result).build();
  }
}
