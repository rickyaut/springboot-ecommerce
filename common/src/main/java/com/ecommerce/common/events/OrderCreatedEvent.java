package com.ecommerce.common.events;

import java.math.BigDecimal;

public class OrderCreatedEvent extends OrderEvent {
    private String customerId;
    private BigDecimal amount;
    
    public OrderCreatedEvent() {}
    
    public OrderCreatedEvent(String orderId, String sagaId, String customerId, BigDecimal amount) {
        super(orderId, sagaId);
        this.customerId = customerId;
        this.amount = amount;
    }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}