package com.ecommerce.erp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/erp")
public class ERPController {
    
    private final Random random = new Random();
    
    @PostMapping("/orders/{orderId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateOrder(@PathVariable String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate ERP processing time (1-10 seconds)
                int processingTime = random.nextInt(9000) + 1000;
                Thread.sleep(processingTime);
                
                // Simulate occasional failures (10% failure rate)
                if (random.nextDouble() < 0.1) {
                    return ResponseEntity.status(500)
                        .body(Map.of("error", "ERP system temporarily unavailable"));
                }
                
                return ResponseEntity.ok(Map.of(
                    "orderId", orderId,
                    "status", "updated",
                    "processingTime", processingTime + "ms"
                ));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ResponseEntity.status(500)
                    .body(Map.of("error", "Processing interrupted"));
            }
        });
    }
    
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable String orderId) {
        return ResponseEntity.ok(Map.of(
            "orderId", orderId,
            "status", "active",
            "lastUpdated", System.currentTimeMillis()
        ));
    }
}