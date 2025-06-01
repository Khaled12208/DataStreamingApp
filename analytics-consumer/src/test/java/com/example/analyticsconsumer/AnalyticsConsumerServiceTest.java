package com.example.analyticsconsumer;

import com.example.analyticsconsumer.model.CustomerStats;
import com.example.analyticsconsumer.model.FraudAlert;
import com.example.analyticsconsumer.model.HourlySales;
import com.example.analyticsconsumer.repository.CustomerStatsRepository;
import com.example.analyticsconsumer.repository.FraudAlertRepository;
import com.example.analyticsconsumer.repository.HourlySalesRepository;
import com.example.analyticsconsumer.service.AnalyticsConsumerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsConsumerServiceTest {
    private HourlySalesRepository hourlySalesRepository;
    private CustomerStatsRepository customerStatsRepository;
    private FraudAlertRepository fraudAlertRepository;
    private AnalyticsConsumerService service;

    @BeforeEach
    void setUp() {
        hourlySalesRepository = Mockito.mock(HourlySalesRepository.class);
        customerStatsRepository = Mockito.mock(CustomerStatsRepository.class);
        fraudAlertRepository = Mockito.mock(FraudAlertRepository.class);
        service = new AnalyticsConsumerService(hourlySalesRepository, customerStatsRepository, fraudAlertRepository);
    }

    @Test
    void testConsume_HourlySales() {
        String json = "{" +
                "\"type\":\"hourly_sales\"," +
                "\"window_start\":\"2024-06-01T00:00:00\"," +
                "\"total_sales\":1000.0," +
                "\"average_order_value\":250.0}";
        service.consume(new ConsumerRecord<>("analytics", 0, 0, null, json));
        ArgumentCaptor<HourlySales> captor = ArgumentCaptor.forClass(HourlySales.class);
        Mockito.verify(hourlySalesRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalSales()).isEqualTo(1000.0);
    }

    @Test
    void testConsume_CustomerStats() {
        String json = "{" +
                "\"type\":\"customer_stats\"," +
                "\"window_start\":\"2024-06-01T00:00:00\"," +
                "\"customer_id\":\"cust-1\"," +
                "\"orders\":5," +
                "\"total_spent\":500.0}";
        service.consume(new ConsumerRecord<>("analytics", 0, 0, null, json));
        ArgumentCaptor<CustomerStats> captor = ArgumentCaptor.forClass(CustomerStats.class);
        Mockito.verify(customerStatsRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo("cust-1");
    }

    @Test
    void testConsume_FraudAlert() {
        String json = "{" +
                "\"type\":\"fraud_alert\"," +
                "\"customer_id\":\"cust-2\"," +
                "\"reason\":\"Order amount > $1000\"," +
                "\"order_id\":\"order-99\"," +
                "\"amount\":1500.0}";
        service.consume(new ConsumerRecord<>("analytics", 0, 0, null, json));
        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        Mockito.verify(fraudAlertRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo("cust-2");
    }
} 