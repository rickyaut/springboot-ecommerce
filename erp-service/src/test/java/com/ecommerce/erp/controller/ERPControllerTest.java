package com.ecommerce.erp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ERPController.class)
class ERPControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getOrderStatus_ShouldReturnOrderInfo() throws Exception {
        mockMvc.perform(get("/erp/orders/order-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    void updateOrder_ShouldAcceptRequest() throws Exception {
        mockMvc.perform(post("/erp/orders/order-123"))
                .andExpect(request().asyncStarted());
    }
}