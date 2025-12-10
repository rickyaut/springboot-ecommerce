package com.ecommerce.payment.listener;

import com.ecommerce.common.events.OrderCreatedEvent;
import com.ecommerce.common.events.OrderEvent;
import com.ecommerce.payment.idempotency.PaymentIdempotencyService;
import com.ecommerce.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {
    
    private final PaymentService paymentService;
    private final PaymentIdempotencyService idempotencyService;
    
    public PaymentEventListener(PaymentService paymentService, PaymentIdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }
    
    @KafkaListener(topics = "payment-requests")
    public void handlePaymentRequest(OrderCreatedEvent event) {
        // Generate idempotency key from order and saga ID
        String idempotencyKey = generateIdempotencyKey(event.getOrderId(), event.getSagaId());
        
        // Check if payment was already processed
        if (idempotencyService.isPaymentProcessed(idempotencyKey)) {
            String result = idempotencyService.getPaymentResult(idempotencyKey);
            // Payment already processed, skip to avoid duplicate charge
            return;
        }
        
        // Process payment and record as processed
        paymentService.processPayment(event);
        idempotencyService.recordPaymentProcessed(idempotencyKey, "PROCESSED");
    }
    
    @KafkaListener(topics = "payment-compensations")
    public void handlePaymentCompensation(OrderEvent event) {
        // Generate idempotency key for compensation
        String compensationKey = generateCompensationKey(event.getOrderId());
        
        // Check if compensation was already processed
        if (idempotencyService.isPaymentProcessed(compensationKey)) {
            // Compensation already processed, skip to avoid double refund
            return;
        }
        
        // Process compensation and record as processed
        paymentService.compensatePayment(event);
        idempotencyService.recordPaymentProcessed(compensationKey, "COMPENSATED");
    }
    
    /**
     * Generate idempotency key from order and saga ID.
     * This ensures the same payment request (identified by order+saga) is only processed once.
     */
    private String generateIdempotencyKey(String orderId, String sagaId) {
        return orderId + ":" + sagaId;
    }
    
    /**
     * Generate compensation key from order ID.
     * This ensures the same compensation request is only processed once.
     */
    private String generateCompensationKey(String orderId) {
        return "compensation:" + orderId;
    }
}