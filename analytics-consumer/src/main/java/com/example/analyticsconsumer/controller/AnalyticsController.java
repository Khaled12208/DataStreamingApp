package com.example.analyticsconsumer.controller;

import com.example.analyticsconsumer.model.*;
import com.example.analyticsconsumer.repository.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
    private final HourlySalesRepository hourlySalesRepository;
    private final CustomerStatsRepository customerStatsRepository;
    private final FraudAlertRepository fraudAlertRepository;

    public AnalyticsController(HourlySalesRepository hourlySalesRepository,
                               CustomerStatsRepository customerStatsRepository,
                               FraudAlertRepository fraudAlertRepository) {
        this.hourlySalesRepository = hourlySalesRepository;
        this.customerStatsRepository = customerStatsRepository;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @GetMapping("/sales")
    public List<HourlySales> getSales() {
        return hourlySalesRepository.findAll();
    }

    @GetMapping("/customers")
    public List<CustomerStats> getCustomers() {
        return customerStatsRepository.findAll();
    }

    @GetMapping("/fraud")
    public List<FraudAlert> getFraud() {
        return fraudAlertRepository.findAll();
    }
} 