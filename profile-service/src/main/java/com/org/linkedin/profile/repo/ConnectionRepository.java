package com.org.linkedin.profile.repo;

import com.org.linkedin.domain.Connection;
import com.org.linkedin.domain.enumeration.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, UUID> {

    List<Connection> findByRequesterIdAndStatus(UUID requesterId, ConnectionStatus status);

    List<Connection> findByReceiverIdAndStatus(UUID receiverId, ConnectionStatus status);

    java.util.Optional<Connection> findByRequesterIdAndReceiverId(UUID requesterId, UUID receiverId);

    @Query("SELECT c FROM Connection c WHERE c.requesterId = :userId OR c.receiverId = :userId")
    List<Connection> findAcceptedConnectionsForUser(@Param("userId") UUID userId);

    @Query("SELECT c FROM Connection c WHERE (c.requesterId IN :userIds OR c.receiverId IN :userIds) AND c.status = 'ACCEPTED'")
    List<Connection> findConnectionsWithUsers(@Param("userIds") List<UUID> userIds);
}
