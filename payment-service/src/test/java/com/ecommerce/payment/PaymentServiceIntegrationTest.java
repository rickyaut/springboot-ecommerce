package com.ecommerce.payment;

import com.ecommerce.common.events.OrderCreatedEvent;
import com.ecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"payment-requests", "payment-responses"})
@DirtiesContext
class PaymentServiceIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void processPayment_ShouldHandlePaymentRequest() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            "order-1", "saga-1", "customer-123", new BigDecimal("99.99")
        );

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            try {
                paymentService.processPayment(event);
            } catch (RuntimeException e) {
                // Random failures are expected in simulation
                assertTrue(e.getMessage().contains("Payment gateway timeout"));
            }
        });
    }

    @Test
    void fallbackPayment_ShouldHandleFailures() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            "order-1", "saga-1", "customer-123", new BigDecimal("99.99")
        );
        RuntimeException exception = new RuntimeException("Test failure");

        // When & Then
        assertDoesNotThrow(() -> paymentService.fallbackPayment(event, exception));
    }
}