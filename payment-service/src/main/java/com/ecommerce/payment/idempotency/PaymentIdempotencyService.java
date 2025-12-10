package com.ecommerce.payment.idempotency;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for managing payment idempotency keys to prevent duplicate charges.
 * Stores processed payment information in Redis with TTL of 24 hours.
 */
@Service
public class PaymentIdempotencyService {
    
    private static final String IDEMPOTENCY_KEY_PREFIX = "payment:idempotency:";
    private static final long TTL_HOURS = 24;
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public PaymentIdempotencyService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Checks if a payment with the given idempotency key has already been processed.
     *
     * @param idempotencyKey unique key for the payment request
     * @return true if payment was already processed, false otherwise
     */
    public boolean isPaymentProcessed(String idempotencyKey) {
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Records a payment as processed in Redis with TTL.
     *
     * @param idempotencyKey unique key for the payment request
     * @param paymentResult the result/status of the payment processing
     */
    public void recordPaymentProcessed(String idempotencyKey, String paymentResult) {
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, paymentResult, TTL_HOURS, TimeUnit.HOURS);
    }
    
    /**
     * Retrieves the stored result of a previously processed payment.
     *
     * @param idempotencyKey unique key for the payment request
     * @return the payment result if found, null otherwise
     */
    public String getPaymentResult(String idempotencyKey) {
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        return redisTemplate.opsForValue().get(key);
    }
    
    /**
     * Removes an idempotency key (useful for testing or manual cleanup).
     *
     * @param idempotencyKey unique key to remove
     */
    public void removePaymentKey(String idempotencyKey) {
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        redisTemplate.delete(key);
    }
}
