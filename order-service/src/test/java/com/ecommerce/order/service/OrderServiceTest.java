package com.ecommerce.order.service;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.event.OrderCreatedApplicationEvent;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, eventPublisher);
    }

    @Test
    void createOrder_ShouldCreateOrderAndPublishEvent() {
        // Given
        String customerId = "customer-123";
        BigDecimal amount = new BigDecimal("99.99");
        Order savedOrder = new Order("order-1", customerId, amount);
        
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        Order result = orderService.createOrder(customerId, amount);

        // Then
        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        assertEquals(amount, result.getAmount());
        verify(orderRepository).save(any(Order.class));
        
        // Verify OrderCreatedApplicationEvent was published
        ArgumentCaptor<OrderCreatedApplicationEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedApplicationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        OrderCreatedApplicationEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.getOrder());
        assertEquals("order-1", publishedEvent.getOrder().getId());
    }

    @Test
    void updateStatus_ShouldUpdateOrderStatus() {
        // Given
        String orderId = "order-1";
        Order order = new Order(orderId, "customer-123", new BigDecimal("99.99"));
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        orderService.updateStatus(orderId, "PAYMENT_PROCESSING");

        // Then
        verify(orderRepository).save(order);
        assertEquals(Order.OrderStatus.PAYMENT_PROCESSING, order.getStatus());
    }

    @Test
    void completeOrder_ShouldSetStatusToCompleted() {
        // Given
        String orderId = "order-1";
        Order order = new Order(orderId, "customer-123", new BigDecimal("99.99"));
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        orderService.completeOrder(orderId);

        // Then
        verify(orderRepository).save(order);
        assertEquals(Order.OrderStatus.COMPLETED, order.getStatus());
    }
}