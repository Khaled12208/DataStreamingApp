package com.example.orderproducer.model;

import jakarta.validation.constraints.*;
import java.util.List;

public class OrderRequest {
    @NotBlank
    private String orderId;

    @NotBlank
    private String customerId;

    @Positive
    private double amount;

    @NotNull
    private Long timestamp;

    @NotEmpty
    private List<@NotBlank String> items;

    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public List<String> getItems() { return items; }
    public void setItems(List<String> items) { this.items = items; }
} 