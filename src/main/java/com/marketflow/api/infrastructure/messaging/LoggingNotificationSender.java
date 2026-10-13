package com.marketflow.api.infrastructure.messaging;

import com.marketflow.api.application.event.Events.NotificationSendEvent;
import com.marketflow.api.application.notification.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Development delivery channel. Replace with an e-mail/push adapter (SES, SendGrid, FCM) in production.
 */
@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(NotificationSendEvent n) {
        log.info("[notification] to={} type={} title='{}' message='{}'", n.userId(), n.type(), n.title(), n.message());
    }
}
