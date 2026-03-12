package com.org.linkedin.notification.controller;

import com.org.linkedin.notification.domain.Notification;
import com.org.linkedin.notification.repo.NotificationRepository;
import com.org.linkedin.utility.client.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${apiPrefix}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    @GetMapping
    public List<Notification> getMyNotifications(Authentication authentication) {
        UUID keycloakId = UUID.fromString(authentication.getName());
        UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(internalUserId);
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
