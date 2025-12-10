package com.ecommerce.common.events;

public class PaymentFailedEvent extends OrderEvent {
    private String reason;
    
    public PaymentFailedEvent() {}
    
    public PaymentFailedEvent(String orderId, String sagaId, String reason) {
        super(orderId, sagaId);
        this.reason = reason;
    }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}