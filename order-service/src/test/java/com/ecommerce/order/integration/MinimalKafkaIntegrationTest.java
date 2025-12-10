package com.ecommerce.order.integration;

import com.ecommerce.common.events.OrderEvent;
import com.ecommerce.common.events.PaymentProcessedEvent;
import com.ecommerce.order.saga.OrderSaga;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;


import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "auth.use-database=false",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.properties.spring.json.trusted.packages=*"
})
@EmbeddedKafka(
    partitions = 1,
    topics = {"payment-responses"},
    brokerProperties = {"auto.create.topics.enable=true"})
@DirtiesContext
class MinimalKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @SpyBean
    private OrderSaga orderSaga;

    @Test
    void shouldSendAndReceiveKafkaMessage() {
        // Given
        PaymentProcessedEvent event = new PaymentProcessedEvent("order-1", "saga-1", "payment-1");

        // When
        kafkaTemplate.send("payment-responses", event);

        // Then - verify Kafka listener received the message
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(orderSaga).handlePaymentResponse(any(OrderEvent.class));
        });
    }
}