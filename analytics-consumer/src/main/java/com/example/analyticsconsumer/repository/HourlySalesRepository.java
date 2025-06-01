package com.example.analyticsconsumer.repository;

import com.example.analyticsconsumer.model.HourlySales;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HourlySalesRepository extends JpaRepository<HourlySales, Long> {
} 