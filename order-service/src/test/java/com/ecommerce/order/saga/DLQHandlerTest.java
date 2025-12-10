package com.ecommerce.order.saga;

import com.ecommerce.common.events.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DLQHandlerTest {

    @InjectMocks
    private DLQHandler dlqHandler;

    @Test
    void handlePaymentDLQ_ShouldProcessFailedMessage() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            "order-1", "saga-1", "customer-123", new BigDecimal("99.99")
        );

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> dlqHandler.handlePaymentDLQ(event));
    }

    @Test
    void handleERPDLQ_ShouldProcessFailedMessage() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            "order-1", "saga-1", "customer-123", new BigDecimal("99.99")
        );

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> dlqHandler.handleERPDLQ(event));
    }
}