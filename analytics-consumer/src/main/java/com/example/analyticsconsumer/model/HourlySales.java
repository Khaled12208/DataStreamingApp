package com.example.analyticsconsumer.model;

import jakarta.persistence.*;

@Entity
@Table(name = "hourly_sales")
public class HourlySales {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "window_start")
    private String windowStart;

    @Column(name = "total_sales")
    private double totalSales;

    @Column(name = "average_order_value")
    private double averageOrderValue;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWindowStart() { return windowStart; }
    public void setWindowStart(String windowStart) { this.windowStart = windowStart; }
    public double getTotalSales() { return totalSales; }
    public void setTotalSales(double totalSales) { this.totalSales = totalSales; }
    public double getAverageOrderValue() { return averageOrderValue; }
    public void setAverageOrderValue(double averageOrderValue) { this.averageOrderValue = averageOrderValue; }
} 