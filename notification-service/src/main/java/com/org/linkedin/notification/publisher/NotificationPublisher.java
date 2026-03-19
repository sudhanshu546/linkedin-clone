package com.org.linkedin.notification.publisher;

import com.org.linkedin.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void pushNotification(Notification notification) {
        messagingTemplate.convertAndSendToUser(
            notification.getRecipientId().toString(), 
            "/queue/notifications", 
            notification
        );
    }
}
