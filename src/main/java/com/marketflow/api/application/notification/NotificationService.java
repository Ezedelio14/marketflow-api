package com.marketflow.api.application.notification;

import com.marketflow.api.application.common.PageResponse;
import com.marketflow.api.application.event.DomainEventFactory;
import com.marketflow.api.application.event.EventPublisher;
import com.marketflow.api.application.event.Events.OrderCancelledEvent;
import com.marketflow.api.application.event.Events.OrderCreatedEvent;
import com.marketflow.api.application.event.Events.OrderPaidEvent;
import com.marketflow.api.application.event.Events.OrderShippedEvent;
import com.marketflow.api.application.event.Events.PaymentFailedEvent;
import com.marketflow.api.application.notification.dto.NotificationResponse;
import com.marketflow.api.domain.common.exception.ResourceNotFoundException;
import com.marketflow.api.domain.notification.Notification;
import com.marketflow.api.domain.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Event handler + query service for notifications: persists the in-app notification and fans it out to
 * {@code notification.send} for external delivery channels.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationFactory factory;
    private final EventPublisher eventPublisher;
    private final DomainEventFactory eventFactory;

    @Transactional
    public void on(OrderCreatedEvent e) { store(factory.create(e)); }

    @Transactional
    public void on(OrderPaidEvent e) { store(factory.create(e)); }

    @Transactional
    public void on(OrderCancelledEvent e) { store(factory.create(e)); }

    @Transactional
    public void on(OrderShippedEvent e) { store(factory.create(e)); }

    @Transactional
    public void on(PaymentFailedEvent e) { store(factory.create(e)); }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, Pageable pageable) {
        return PageResponse.from(repository.findByUserIdOrderByCreatedAtDesc(userId, pageable), NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = repository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    private void store(Notification notification) {
        repository.save(notification);
        eventPublisher.publish(eventFactory.notificationSend(notification));
    }
}
