package com.marketflow.api.application.event;

/**
 * Output port for integration events. When called inside a transaction, implementations must defer the send
 * until after commit so consumers never observe uncommitted state.
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
