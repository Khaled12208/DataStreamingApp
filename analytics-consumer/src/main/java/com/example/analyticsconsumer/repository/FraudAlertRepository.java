package com.example.analyticsconsumer.repository;

import com.example.analyticsconsumer.model.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
} 