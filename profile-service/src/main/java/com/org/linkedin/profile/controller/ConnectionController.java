package com.org.linkedin.profile.controller;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.connection.ConnectionDTO;
import com.org.linkedin.dto.connection.ConnectionRequestDTO;
import com.org.linkedin.dto.connection.UserConnectionStatusDTO;
import com.org.linkedin.profile.service.ConnectionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${apiPrefix}/connections")
@RequiredArgsConstructor
public class ConnectionController {

  private final ConnectionService connectionService;

  @PostMapping("/request")
  public ResponseEntity<ApiResponse<Void>> sendRequest(
      Authentication authentication, @RequestBody ConnectionRequestDTO dto) {
    connectionService.sendRequest(authentication, dto.getReceiverId());
    return ResponseEntity.ok(ApiResponse.success("Connection request sent", null));
  }

  @PostMapping("/{id}/respond")
  public ResponseEntity<ApiResponse<Void>> respond(
      Authentication authentication,
      @PathVariable UUID id,
      @RequestParam("accept") boolean accept) {
    connectionService.respondToRequest(authentication, id, accept);
    return ResponseEntity.ok(
        ApiResponse.success(accept ? "Connection accepted" : "Connection rejected", null));
  }

  @DeleteMapping("/{id}/cancel")
  public ResponseEntity<ApiResponse<Void>> cancelRequest(
      Authentication authentication, @PathVariable UUID id) {
    connectionService.cancelRequest(authentication, id);
    return ResponseEntity.ok(ApiResponse.success("Connection request cancelled", null));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<ConnectionDTO>>> myConnections(
      Authentication authentication) {
    List<ConnectionDTO> result = connectionService.getMyConnections(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @GetMapping("/pending")
  public ResponseEntity<ApiResponse<List<ConnectionDTO>>> getPending(
      Authentication authentication) {
    List<ConnectionDTO> result = connectionService.getPendingRequests(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @GetMapping("/status/{otherUserId}")
  public ResponseEntity<ApiResponse<UserConnectionStatusDTO>> getStatus(
      Authentication authentication, @PathVariable UUID otherUserId) {
    UserConnectionStatusDTO result =
        connectionService.getConnectionStatus(authentication, otherUserId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @GetMapping("/recommendations")
  public ResponseEntity<ApiResponse<List<UUID>>> getRecommendations(Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());
    List<UUID> result = connectionService.findMutualConnections(userId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }
}
