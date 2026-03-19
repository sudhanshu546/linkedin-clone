package com.org.linkedin.notification.mapper;

import com.org.linkedin.dto.notification.NotificationDTO;
import com.org.linkedin.notification.domain.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper extends EntityMapper<NotificationDTO, Notification> {
}
