package com.ecommerce.order.event;

import com.ecommerce.order.entity.Order;
import org.springframework.context.ApplicationEvent;

/**
 * Application event published when an order is created.
 * This event is used to initiate the saga without creating circular dependencies.
 */
public class OrderCreatedApplicationEvent extends ApplicationEvent {
    
    private final Order order;
    
    public OrderCreatedApplicationEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }
    
    public Order getOrder() {
        return order;
    }
}
