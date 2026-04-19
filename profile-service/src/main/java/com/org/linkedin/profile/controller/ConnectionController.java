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

  /**
   * Sends a connection request to another user.
   *
   * @param authentication The authenticated user security context.
   * @param dto The data transfer object containing the receiver's unique identifier.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @PostMapping("/request")
  public ResponseEntity<ApiResponse<Void>> sendRequest(
      Authentication authentication, @RequestBody ConnectionRequestDTO dto) {
    connectionService.sendRequest(authentication, dto.getReceiverId());
    return ResponseEntity.ok(ApiResponse.success("Connection request sent", null));
  }

  /**
   * Responds to a pending connection request by accepting or rejecting it.
   *
   * @param authentication The authenticated user security context.
   * @param id The unique identifier of the connection request to respond to.
   * @param accept A boolean flag indicating whether to accept (true) or reject (false) the request.
   * @return A ResponseEntity containing an ApiResponse indicating success with a descriptive
   *     message.
   */
  @PostMapping("/{id}/respond")
  public ResponseEntity<ApiResponse<Void>> respond(
      Authentication authentication,
      @PathVariable UUID id,
      @RequestParam("accept") boolean accept) {
    connectionService.respondToRequest(authentication, id, accept);
    return ResponseEntity.ok(
        ApiResponse.success(accept ? "Connection accepted" : "Connection rejected", null));
  }

  /**
   * Cancels a previously sent connection request.
   *
   * @param authentication The authenticated user security context.
   * @param id The unique identifier of the connection request to cancel.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @DeleteMapping("/{id}/cancel")
  public ResponseEntity<ApiResponse<Void>> cancelRequest(
      Authentication authentication, @PathVariable UUID id) {
    connectionService.cancelRequest(authentication, id);
    return ResponseEntity.ok(ApiResponse.success("Connection request cancelled", null));
  }

  /**
   * Retrieves a list of all active connections for the current authenticated user.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with a list of ConnectionDTOs.
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<ConnectionDTO>>> myConnections(
      Authentication authentication) {
    List<ConnectionDTO> result = connectionService.getMyConnections(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Retrieves all pending connection requests received by the current authenticated user.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with a list of pending ConnectionDTOs.
   */
  @GetMapping("/pending")
  public ResponseEntity<ApiResponse<List<ConnectionDTO>>> getPending(
      Authentication authentication) {
    List<ConnectionDTO> result = connectionService.getPendingRequests(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Retrieves the current connection status between the authenticated user and another user.
   *
   * @param authentication The authenticated user security context.
   * @param otherUserId The unique identifier of the other user.
   * @return A ResponseEntity containing an ApiResponse with a UserConnectionStatusDTO.
   */
  @GetMapping("/status/{otherUserId}")
  public ResponseEntity<ApiResponse<UserConnectionStatusDTO>> getStatus(
      Authentication authentication, @PathVariable UUID otherUserId) {
    UserConnectionStatusDTO result =
        connectionService.getConnectionStatus(authentication, otherUserId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Retrieves a list of recommended users based on mutual connections (People you may know).
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with a list of recommended TUserDTOs.
   */
  @GetMapping("/recommendations")
  public ResponseEntity<ApiResponse<List<com.org.linkedin.dto.user.TUserDTO>>> getRecommendations(
      Authentication authentication) {
    List<com.org.linkedin.dto.user.TUserDTO> result =
        connectionService.getNetworkSuggestions(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Retrieves a list of mutual connections between the current user and another user.
   *
   * @param authentication The authenticated user security context.
   * @param otherUserId The unique identifier of the other user.
   * @return A ResponseEntity containing an ApiResponse with a list of mutual connection user IDs.
   */
  @GetMapping("/mutual/{otherUserId}")
  public ResponseEntity<ApiResponse<List<UUID>>> getMutual(
      Authentication authentication, @PathVariable UUID otherUserId) {
    List<UUID> result = connectionService.findMutualConnections(authentication, otherUserId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }
}
