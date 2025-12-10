package com.ecommerce.common.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "ORDER_CREATED"),
    @JsonSubTypes.Type(value = PaymentProcessedEvent.class, name = "PAYMENT_PROCESSED"),
    @JsonSubTypes.Type(value = PaymentFailedEvent.class, name = "PAYMENT_FAILED"),
    @JsonSubTypes.Type(value = ERPUpdatedEvent.class, name = "ERP_UPDATED"),
    @JsonSubTypes.Type(value = ERPFailedEvent.class, name = "ERP_FAILED"),
    @JsonSubTypes.Type(value = OrderCompletedEvent.class, name = "ORDER_COMPLETED"),
    @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "ORDER_CANCELLED")
})
public abstract class OrderEvent {
    private String orderId;
    private String sagaId;
    
    public OrderEvent() {}
    
    public OrderEvent(String orderId, String sagaId) {
        this.orderId = orderId;
        this.sagaId = sagaId;
    }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
}