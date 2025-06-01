package com.example.analyticsconsumer.model;

import jakarta.persistence.*;

@Entity
@Table(name = "customer_stats")
public class CustomerStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "window_start")
    private String windowStart;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "orders")
    private int orders;

    @Column(name = "total_spent")
    private double totalSpent;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWindowStart() { return windowStart; }
    public void setWindowStart(String windowStart) { this.windowStart = windowStart; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public int getOrders() { return orders; }
    public void setOrders(int orders) { this.orders = orders; }
    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }
} 