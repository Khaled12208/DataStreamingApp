package com.example.analyticsconsumer.repository;

import com.example.analyticsconsumer.model.CustomerStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerStatsRepository extends JpaRepository<CustomerStats, Long> {
} 