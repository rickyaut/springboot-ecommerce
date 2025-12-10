package com.ecommerce.order.service;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.event.OrderCreatedApplicationEvent;
import com.ecommerce.order.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    public OrderService(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }
    
    public Order createOrder(String customerId, BigDecimal amount) {
        Order order = new Order(UUID.randomUUID().toString(), customerId, amount);
        order.setSagaId(UUID.randomUUID().toString());
        order = orderRepository.save(order);
        
        // Publish event to initiate saga
        eventPublisher.publishEvent(new OrderCreatedApplicationEvent(this, order));
        return order;
    }
    
    public void updateStatus(String orderId, String status) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.valueOf(status));
            orderRepository.save(order);
        });
    }
    
    public void completeOrder(String orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.COMPLETED);
            orderRepository.save(order);
        });
    }
    
    public void cancelOrder(String orderId, String reason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
        });
    }
    
    public java.util.List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    public java.util.Optional<Order> getOrder(String id) {
        return orderRepository.findById(id);
    }
}