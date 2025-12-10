package com.ecommerce.order.saga;

import com.ecommerce.common.events.*;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.event.OrderCreatedApplicationEvent;
import com.ecommerce.order.service.OrderService;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "kafka.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OrderSaga {
    
    private final OrderService orderService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public OrderSaga(OrderService orderService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @EventListener
    @Retry(name = "saga-operations", fallbackMethod = "onOrderCreatedFallback")
    public void onOrderCreated(OrderCreatedApplicationEvent event) {
        startSaga(event.getOrder());
    }
    
    public void onOrderCreatedFallback(OrderCreatedApplicationEvent event, Exception ex) {
        Order order = event.getOrder();
        OrderCreatedEvent orderEvent = new OrderCreatedEvent(
            order.getId(), order.getSagaId(), order.getCustomerId(), order.getAmount()
        );
        kafkaTemplate.send("saga-operations-dlq", orderEvent);
        orderService.cancelOrder(order.getId(), "Saga start failed after retries: " + ex.getMessage());
    }
    
    private void startSaga(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(), order.getSagaId(), order.getCustomerId(), order.getAmount()
        );
        kafkaTemplate.send("payment-requests", event);
    }
    

    
    @KafkaListener(topics = "payment-responses", concurrency = "1")
    @Retry(name = "saga-operations", fallbackMethod = "handlePaymentResponseFallback")
    public void handlePaymentResponse(OrderEvent event) {
        if (event instanceof PaymentProcessedEvent) {
            orderService.updateStatus(event.getOrderId(), "PAYMENT_PROCESSING");
            // Call ERP service directly via HTTP
            callERPService(event.getOrderId());
        } else if (event instanceof PaymentFailedEvent) {
            orderService.cancelOrder(event.getOrderId(), ((PaymentFailedEvent) event).getReason());
        }
    }
    
    private void callERPService(String orderId) {
        try {
            // TODO: Make actual HTTP call to ERP service
            // For now, simulate ERP success
            orderService.completeOrder(orderId);
        } catch (Exception ex) {
            // ERP failed, compensate payment
            kafkaTemplate.send("payment-compensations", new ERPFailedEvent(orderId, "", "ERP service failed"));
            orderService.cancelOrder(orderId, "ERP service failed: " + ex.getMessage());
        }
    }
    
    public void handlePaymentResponseFallback(OrderEvent event, Exception ex) {
        kafkaTemplate.send("saga-operations-dlq", event);
        orderService.cancelOrder(event.getOrderId(), "Payment response handling failed after retries: " + ex.getMessage());
    }
    

}