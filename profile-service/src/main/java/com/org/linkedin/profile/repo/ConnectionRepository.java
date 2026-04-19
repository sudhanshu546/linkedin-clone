package com.org.linkedin.profile.repo;

import com.org.linkedin.domain.Connection;
import com.org.linkedin.domain.enumeration.ConnectionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, UUID> {

  List<Connection> findByRequesterIdAndStatus(UUID requesterId, ConnectionStatus status);

  List<Connection> findByReceiverIdAndStatus(UUID receiverId, ConnectionStatus status);

  List<Connection> findByRequesterId(UUID requesterId);

  List<Connection> findByReceiverId(UUID receiverId);

  java.util.Optional<Connection> findByRequesterIdAndReceiverId(UUID requesterId, UUID receiverId);

  @Query("SELECT c FROM Connection c WHERE c.requesterId = :userId OR c.receiverId = :userId")
  List<Connection> findAcceptedConnectionsForUser(@Param("userId") UUID userId);

  @Query(
      "SELECT c FROM Connection c WHERE (c.requesterId IN :userIds OR c.receiverId IN :userIds) AND c.status = 'ACCEPTED'")
  List<Connection> findConnectionsWithUsers(@Param("userIds") List<UUID> userIds);

  @Query(
      value =
          "SELECT DISTINCT CASE WHEN c.requester_id = sub.friend_id THEN c.receiver_id ELSE c.requester_id END "
              + "FROM connections c "
              + "JOIN ( "
              + "  SELECT CASE WHEN c1.requester_id = :userId THEN c1.receiver_id ELSE c1.requester_id END as friend_id "
              + "  FROM connections c1 "
              + "  WHERE (c1.requester_id = :userId OR c1.receiver_id = :userId) AND c1.status = 'ACCEPTED' "
              + ") sub ON (c.requester_id = sub.friend_id OR c.receiver_id = sub.friend_id) "
              + "WHERE c.status = 'ACCEPTED' "
              + "AND c.requester_id != :userId AND c.receiver_id != :userId "
              + "AND NOT EXISTS ( "
              + "  SELECT 1 FROM connections c2 "
              + "  WHERE (c2.requester_id = :userId AND c2.receiver_id = (CASE WHEN c.requester_id = sub.friend_id THEN c.receiver_id ELSE c.requester_id END)) "
              + "     OR (c2.receiver_id = :userId AND c2.requester_id = (CASE WHEN c.requester_id = sub.friend_id THEN c.receiver_id ELSE c.requester_id END)) "
              + ") LIMIT 20",
      nativeQuery = true)
  List<UUID> findPeopleYouMayKnow(@Param("userId") UUID userId);
}
