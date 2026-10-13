package com.marketflow.api.infrastructure.messaging.consumer;

import com.marketflow.api.application.event.Events.NotificationSendEvent;
import com.marketflow.api.application.event.KafkaTopics;
import com.marketflow.api.application.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationDispatchConsumer {

    private final NotificationSender sender;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_SEND, groupId = "marketflow-notification-dispatch")
    public void onNotification(NotificationSendEvent event) {
        sender.send(event);
    }
}
