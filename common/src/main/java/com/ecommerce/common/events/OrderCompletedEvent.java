package com.ecommerce.common.events;

public class OrderCompletedEvent extends OrderEvent {
    public OrderCompletedEvent() {}
    
    public OrderCompletedEvent(String orderId, String sagaId) {
        super(orderId, sagaId);
    }
}