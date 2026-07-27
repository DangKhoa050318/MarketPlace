package com.training.marketplace.messaging.event;

public interface StockEvent {

    String schemaVersion();

    String eventId();

    Long movementId();
}
