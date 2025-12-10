package com.ecommerce.common.events;

public class PaymentProcessedEvent extends OrderEvent {
    private String paymentId;
    
    public PaymentProcessedEvent() {}
    
    public PaymentProcessedEvent(String orderId, String sagaId, String paymentId) {
        super(orderId, sagaId);
        this.paymentId = paymentId;
    }
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
}

