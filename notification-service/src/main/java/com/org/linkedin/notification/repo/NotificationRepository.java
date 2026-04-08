package com.org.linkedin.notification.repo;

import com.org.linkedin.notification.domain.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  List<Notification> findByRecipientIdOrderByCreatedDateDesc(UUID recipientId);

  List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedDateDesc(UUID recipientId);

  long countByRecipientIdAndIsReadFalse(UUID recipientId);

  void deleteByRecipientId(UUID recipientId);
}
