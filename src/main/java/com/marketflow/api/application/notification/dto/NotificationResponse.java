package com.marketflow.api.application.notification.dto;

import com.marketflow.api.domain.notification.Notification;
import com.marketflow.api.domain.notification.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "In-app notification")
public record NotificationResponse(
        @Schema(description = "Notification id") UUID id,
        @Schema(description = "Type", example = "ORDER_PAID") NotificationType type,
        @Schema(description = "Title", example = "Payment confirmed for MF-261013-7Q2K9XPA") String title,
        @Schema(description = "Message body") String message,
        @Schema(description = "When it was read (null = unread)") Instant readAt,
        @Schema(description = "Creation instant") Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getReadAt(), n.getCreatedAt());
    }
}
