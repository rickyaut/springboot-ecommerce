package com.ecommerce.payment.service;

import com.ecommerce.common.events.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();
    
    public PaymentService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @CircuitBreaker(name = "payment-processor", fallbackMethod = "fallbackPayment")
    @Retry(name = "payment-processor")
    public void processPayment(OrderCreatedEvent event) {
        // Simulate payment processing with potential failures
        if (random.nextDouble() < 0.2) { // 20% failure rate
            throw new RuntimeException("Payment gateway timeout");
        }
        
        // Simulate processing time
        try {
            Thread.sleep(random.nextInt(2000) + 500); // 0.5-2.5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        PaymentProcessedEvent response = new PaymentProcessedEvent(
            event.getOrderId(), event.getSagaId(), UUID.randomUUID().toString()
        );
        kafkaTemplate.send("payment-responses", response);
    }
    
    public void fallbackPayment(OrderCreatedEvent event, Exception ex) {
        PaymentFailedEvent response = new PaymentFailedEvent(
            event.getOrderId(), event.getSagaId(), "Payment service unavailable: " + ex.getMessage()
        );
        kafkaTemplate.send("payment-responses", response);
    }
    
    public void compensatePayment(OrderEvent event) {
        // Simulate payment compensation (refund)
        System.out.println("Compensating payment for order: " + event.getOrderId());
    }
}