package com.ecommerce.common.events;

public class ERPUpdatedEvent extends OrderEvent {
    public ERPUpdatedEvent() {}
    
    public ERPUpdatedEvent(String orderId, String sagaId) {
        super(orderId, sagaId);
    }
}