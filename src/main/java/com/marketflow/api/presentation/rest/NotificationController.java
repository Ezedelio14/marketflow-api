package com.marketflow.api.presentation.rest;

import com.marketflow.api.application.common.PageResponse;
import com.marketflow.api.application.notification.NotificationService;
import com.marketflow.api.application.notification.dto.NotificationResponse;
import com.marketflow.api.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping(path = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "In-app notifications produced from Kafka events")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List my notifications", description = "Newest first.", operationId = "listNotifications")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of notifications"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<NotificationResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                                   @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.list(user.id(), pageable);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread notifications", operationId = "countUnreadNotifications")
    @ApiResponse(responseCode = "200", description = "Unread count",
            content = @Content(schema = @Schema(example = "{\"unread\": 3}")))
    public Map<String, Long> unreadCount(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return Map.of("unread", notificationService.unreadCount(user.id()));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark as read", operationId = "markNotificationRead")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Notification not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NotificationResponse markRead(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                         @Parameter(description = "Notification id", required = true) @PathVariable UUID notificationId) {
        return notificationService.markRead(user.id(), notificationId);
    }
}
