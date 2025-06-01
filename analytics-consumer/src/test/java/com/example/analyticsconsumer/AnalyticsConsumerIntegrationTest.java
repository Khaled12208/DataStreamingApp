package com.example.analyticsconsumer;

import com.example.analyticsconsumer.model.HourlySales;
import com.example.analyticsconsumer.repository.HourlySalesRepository;
import com.example.analyticsconsumer.repository.CustomerStatsRepository;
import com.example.analyticsconsumer.repository.FraudAlertRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@EmbeddedKafka(partitions = 1, topics = {"analytics"})
@DirtiesContext
class AnalyticsConsumerIntegrationTest {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @MockBean
    private HourlySalesRepository hourlySalesRepository;

    @MockBean
    private CustomerStatsRepository customerStatsRepository;

    @MockBean
    private FraudAlertRepository fraudAlertRepository;
    
    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void testAnalyticsConsumed() throws Exception {
        // Setup mock
        HourlySales expectedSales = new HourlySales();
        expectedSales.setWindowStart("2024-06-01T00:00:00");
        expectedSales.setTotalSales(1000.0);
        expectedSales.setAverageOrderValue(250.0);
        when(hourlySalesRepository.save(any(HourlySales.class))).thenReturn(expectedSales);

        // Send message to Kafka
        String json = "{" +
                "\"type\":\"hourly_sales\"," +
                "\"window_start\":\"2024-06-01T00:00:00\"," +
                "\"total_sales\":1000.0," +
                "\"average_order_value\":250.0}";
        kafkaTemplate.send(new ProducerRecord<>("analytics", null, json));
        
        // Verify repository was called with timeout
        verify(hourlySalesRepository, timeout(5000)).save(any(HourlySales.class));
    }
}