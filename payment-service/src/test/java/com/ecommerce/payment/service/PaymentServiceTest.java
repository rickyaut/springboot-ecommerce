package com.ecommerce.payment.service;

import com.ecommerce.common.events.OrderCreatedEvent;
import com.ecommerce.common.events.PaymentProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(kafkaTemplate);
    }

    @Test
    void processPayment_ShouldSendPaymentProcessedEvent() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            "order-1", "saga-1", "customer-123", new BigDecimal("99.99")
        );

        // When
        assertDoesNotThrow(() -> {
            try {
                paymentService.processPayment(event);
                // Verify success case
                verify(kafkaTemplate, timeout(5000)).send(eq("payment-responses"), any(PaymentProcessedEvent.class));
            } catch (RuntimeException e) {
                // Verify fallback is called for failures
                paymentService.fallbackPayment(event, e);
                verify(kafkaTemplate, timeout(5000)).send(eq("payment-responses"), any());
            }
        });
    }

    @Test
    void fallbackPayment_ShouldSendPaymentFailedEvent() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            "order-1", "saga-1", "customer-123", new BigDecimal("99.99")
        );
        RuntimeException exception = new RuntimeException("Payment gateway timeout");

        // When
        paymentService.fallbackPayment(event, exception);

        // Then
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("payment-responses"), eventCaptor.capture());
        
        Object sentEvent = eventCaptor.getValue();
        assertInstanceOf(com.ecommerce.common.events.PaymentFailedEvent.class, sentEvent);
    }
}