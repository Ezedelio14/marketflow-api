package com.marketflow.api.application.notification;

import com.marketflow.api.application.event.Events.NotificationSendEvent;

/**
 * Output port for delivery channels (e-mail, push, SMS).
 */
public interface NotificationSender {

    void send(NotificationSendEvent notification);
}
