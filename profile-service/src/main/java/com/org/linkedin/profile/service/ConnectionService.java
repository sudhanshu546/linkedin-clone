package com.org.linkedin.profile.service;

import com.org.linkedin.dto.connection.ConnectionDTO;
import com.org.linkedin.dto.connection.UserConnectionStatusDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface ConnectionService {
  List<ConnectionDTO> getMyConnections(Authentication authentication);

  void respondToRequest(Authentication authentication, UUID id, boolean accept);

  void sendRequest(Authentication authentication, UUID receiverId);

  void cancelRequest(Authentication authentication, UUID connectionId);

  List<ConnectionDTO> getPendingRequests(Authentication authentication);

  List<UUID> findMutualConnections(UUID userId);

  List<UUID> findMutualConnections(Authentication authentication, UUID otherUserId);

  List<com.org.linkedin.dto.user.TUserDTO> getNetworkSuggestions(Authentication authentication);

  UserConnectionStatusDTO getConnectionStatus(Authentication authentication, UUID otherUserId);
}
