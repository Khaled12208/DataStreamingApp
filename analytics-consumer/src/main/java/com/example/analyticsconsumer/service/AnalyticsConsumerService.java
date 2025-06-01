package com.example.analyticsconsumer.service;

import com.example.analyticsconsumer.model.*;
import com.example.analyticsconsumer.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsConsumerService.class);
    private final HourlySalesRepository hourlySalesRepository;
    private final CustomerStatsRepository customerStatsRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalyticsConsumerService(HourlySalesRepository hourlySalesRepository,
                                    CustomerStatsRepository customerStatsRepository,
                                    FraudAlertRepository fraudAlertRepository) {
        this.hourlySalesRepository = hourlySalesRepository;
        this.customerStatsRepository = customerStatsRepository;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @KafkaListener(topics = "analytics", groupId = "analytics-consumer")
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        String value = record.value();
        try {
            JsonNode node = objectMapper.readTree(value);
            String type = node.get("type").asText();
            switch (type) {
                case "hourly_sales" -> {
                    HourlySales sales = new HourlySales();
                    sales.setWindowStart(node.get("window_start").asText());
                    sales.setTotalSales(node.get("total_sales").asDouble());
                    sales.setAverageOrderValue(node.get("average_order_value").asDouble());
                    hourlySalesRepository.save(sales);
                }
                case "customer_stats" -> {
                    CustomerStats stats = new CustomerStats();
                    stats.setWindowStart(node.get("window_start").asText());
                    stats.setCustomerId(node.get("customer_id").asText());
                    stats.setOrders(node.get("orders").asInt());
                    stats.setTotalSpent(node.get("total_spent").asDouble());
                    customerStatsRepository.save(stats);
                }
                case "fraud_alert" -> {
                    FraudAlert alert = new FraudAlert();
                    alert.setCustomerId(node.get("customer_id").asText());
                    alert.setReason(node.get("reason").asText());
                    if (node.has("order_id")) alert.setOrderId(node.get("order_id").asText());
                    if (node.has("amount")) alert.setAmount(node.get("amount").asDouble());
                    if (node.has("order_count")) alert.setOrderCount(node.get("order_count").asInt());
                    fraudAlertRepository.save(alert);
                }
                default -> logger.warn("Unknown analytics type: {}", type);
            }
        } catch (Exception e) {
            logger.error("Failed to process analytics message: {}", value, e);
        }
    }
} 