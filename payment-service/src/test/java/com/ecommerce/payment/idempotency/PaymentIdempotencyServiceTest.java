package com.ecommerce.payment.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentIdempotencyServiceTest {
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    private PaymentIdempotencyService idempotencyService;
    
    @BeforeEach
    void setUp() {
        idempotencyService = new PaymentIdempotencyService(redisTemplate);
    }
    
    @Test
    void isPaymentProcessed_ShouldReturnTrueWhenKeyExists() {
        // Given
        String idempotencyKey = "order-123:saga-456";
        when(redisTemplate.hasKey("payment:idempotency:" + idempotencyKey)).thenReturn(true);
        
        // When
        boolean result = idempotencyService.isPaymentProcessed(idempotencyKey);
        
        // Then
        assertTrue(result);
        verify(redisTemplate).hasKey("payment:idempotency:" + idempotencyKey);
    }
    
    @Test
    void isPaymentProcessed_ShouldReturnFalseWhenKeyDoesNotExist() {
        // Given
        String idempotencyKey = "order-123:saga-456";
        when(redisTemplate.hasKey("payment:idempotency:" + idempotencyKey)).thenReturn(false);
        
        // When
        boolean result = idempotencyService.isPaymentProcessed(idempotencyKey);
        
        // Then
        assertFalse(result);
        verify(redisTemplate).hasKey("payment:idempotency:" + idempotencyKey);
    }
    
    @Test
    void recordPaymentProcessed_ShouldStoreResultWithTTL() {
        // Given
        String idempotencyKey = "order-123:saga-456";
        String paymentResult = "PROCESSED";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // When
        idempotencyService.recordPaymentProcessed(idempotencyKey, paymentResult);
        
        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(
            "payment:idempotency:" + idempotencyKey,
            paymentResult,
            24,
            TimeUnit.HOURS
        );
    }
    
    @Test
    void getPaymentResult_ShouldReturnStoredResult() {
        // Given
        String idempotencyKey = "order-123:saga-456";
        String expectedResult = "PROCESSED";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("payment:idempotency:" + idempotencyKey))
            .thenReturn(expectedResult);
        
        // When
        String result = idempotencyService.getPaymentResult(idempotencyKey);
        
        // Then
        assertEquals(expectedResult, result);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get("payment:idempotency:" + idempotencyKey);
    }
    
    @Test
    void removePaymentKey_ShouldDeleteKey() {
        // Given
        String idempotencyKey = "order-123:saga-456";
        
        // When
        idempotencyService.removePaymentKey(idempotencyKey);
        
        // Then
        verify(redisTemplate).delete("payment:idempotency:" + idempotencyKey);
    }
}

