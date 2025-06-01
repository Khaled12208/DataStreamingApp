package com.example.analyticsconsumer;

import com.example.analyticsconsumer.controller.AnalyticsController;
import com.example.analyticsconsumer.model.CustomerStats;
import com.example.analyticsconsumer.model.FraudAlert;
import com.example.analyticsconsumer.model.HourlySales;
import com.example.analyticsconsumer.repository.CustomerStatsRepository;
import com.example.analyticsconsumer.repository.FraudAlertRepository;
import com.example.analyticsconsumer.repository.HourlySalesRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private HourlySalesRepository hourlySalesRepository;
    @MockBean
    private CustomerStatsRepository customerStatsRepository;
    @MockBean
    private FraudAlertRepository fraudAlertRepository;

    @Test
    void testGetSales() throws Exception {
        Mockito.when(hourlySalesRepository.findAll()).thenReturn(List.of(new HourlySales()));
        mockMvc.perform(get("/analytics/sales").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testGetCustomers() throws Exception {
        Mockito.when(customerStatsRepository.findAll()).thenReturn(List.of(new CustomerStats()));
        mockMvc.perform(get("/analytics/customers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testGetFraud() throws Exception {
        Mockito.when(fraudAlertRepository.findAll()).thenReturn(List.of(new FraudAlert()));
        mockMvc.perform(get("/analytics/fraud").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
} 