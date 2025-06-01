package com.example.orderproducer.controller;

import com.example.orderproducer.model.OrderRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OrderController(KafkaTemplate<String, String> kafkaTemplate,
                          @Value("${order.topic:orders}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        try {
            // Serialize orderRequest to JSON
            String orderJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(orderRequest);
            kafkaTemplate.send(topic, orderRequest.getOrderId(), orderJson);
            logger.info("Order published: {}", orderJson);
            return ResponseEntity.ok().body("Order received and published");
        } catch (Exception e) {
            logger.error("Failed to publish order", e);
            return ResponseEntity.internalServerError().body("Failed to process order");
        }
    }
} 