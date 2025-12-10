package com.ecommerce.order.saga;

import com.ecommerce.common.events.OrderEvent;
import com.ecommerce.order.notification.SlackNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DLQHandler {
    
    private final SlackNotificationService slackService;
    
    public DLQHandler(@Autowired(required = false) SlackNotificationService slackService) {
        this.slackService = slackService;
        System.out.println("DLQHandler created with SlackService: " + (slackService != null));
    }
    
    @KafkaListener(topics = "payment-responses-dlq")
    public void handlePaymentDLQ(OrderEvent event) {
        System.err.println("Payment processing failed permanently for order: " + event.getOrderId());
        if (slackService != null) {
            slackService.sendDLQAlert("payment-responses-dlq", event.getOrderId(), "Payment processing failed permanently");
        } else {
            System.err.println("SlackService is null - cannot send alert");
        }
    }
    
    @KafkaListener(topics = "erp-responses-dlq")
    public void handleERPDLQ(OrderEvent event) {
        System.err.println("ERP processing failed permanently for order: " + event.getOrderId());
        if (slackService != null) {
            slackService.sendDLQAlert("erp-responses-dlq", event.getOrderId(), "ERP processing failed permanently");
        } else {
            System.err.println("SlackService is null - cannot send alert");
        }
    }
    
    @KafkaListener(topics = "saga-start-dlq")
    public void handleSagaStartDLQ(OrderEvent event) {
        System.err.println("Saga start failed permanently for order: " + event.getOrderId());
        if (slackService != null) {
            slackService.sendDLQAlert("saga-start-dlq", event.getOrderId(), "Saga start failed permanently");
        } else {
            System.err.println("SlackService is null - cannot send alert");
        }
    }
}