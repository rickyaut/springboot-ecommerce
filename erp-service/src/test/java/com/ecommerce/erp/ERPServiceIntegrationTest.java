package com.ecommerce.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ERPServiceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void updateOrder_ShouldProcessAsynchronously() {
        // When & Then
        assertDoesNotThrow(() -> {
            restTemplate.postForEntity("/erp/orders/order-123", null, String.class);
        });
    }

    @Test
    void getOrderStatus_ShouldReturnStatus() {
        // When
        var response = restTemplate.getForEntity("/erp/orders/order-123", String.class);
        
        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("order-123"));
    }
}