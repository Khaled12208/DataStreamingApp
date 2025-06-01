package com.example.streamprocessor;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import java.io.Serializable;
import java.util.List;

public class OrderEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String orderId;
    private String customerId;
    private double amount;
    private Long timestamp;
    private List<String> items;

    // Default constructor required by Flink
    public OrderEvent() {}

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