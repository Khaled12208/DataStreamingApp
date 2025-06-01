package com.example.orderproducer;

import com.example.orderproducer.controller.OrderController;
import com.example.orderproducer.model.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@ImportAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;

    @TestConfiguration
    static class NoOpKafkaTemplateConfig {
        @Bean
        public KafkaTemplate<String, String> kafkaTemplate() {
            ProducerFactory<String, String> fakeFactory = new ProducerFactory<>() {
                @Override
                public Producer<String, String> createProducer() {
                    return new Producer<>() {
                        @Override
                        public void initTransactions() {}
                        
                        @Override
                        public void beginTransaction() {}
                        
                        @Override
                        public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, ConsumerGroupMetadata groupMetadata) {}
                        
                        @Override
                        public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, String consumerGroupId) {}
                        
                        @Override
                        public void commitTransaction() {}
                        
                        @Override
                        public void abortTransaction() {}
                        
                        @Override
                        public java.util.concurrent.Future<RecordMetadata> send(ProducerRecord<String, String> record) {
                            RecordMetadata metadata = new RecordMetadata(new TopicPartition("test", 0), 0L, 0L, 0L, 0L, 0, 0);
                            return CompletableFuture.completedFuture(metadata);
                        }
                        
                        @Override
                        public java.util.concurrent.Future<RecordMetadata> send(ProducerRecord<String, String> record, Callback callback) {
                            RecordMetadata metadata = new RecordMetadata(new TopicPartition("test", 0), 0L, 0L, 0L, 0L, 0, 0);
                            if (callback != null) {
                                callback.onCompletion(metadata, null);
                            }
                            return CompletableFuture.completedFuture(metadata);
                        }
                        
                        @Override
                        public void flush() {}
                        
                        @Override
                        public List<PartitionInfo> partitionsFor(String topic) {
                            return Collections.emptyList();
                        }
                        
                        @Override
                        public Map<MetricName, ? extends Metric> metrics() {
                            return Collections.emptyMap();
                        }
                        
                        @Override
                        public void close() {}
                        
                        @Override
                        public void close(Duration timeout) {}
                    };
                }
                
                @Override
                public boolean transactionCapable() {
                    return false;
                }
                
                @Override
                public void closeThreadBoundProducer() {}
                
                @Override
                public void reset() {}
            };
            
            return new KafkaTemplate<>(fakeFactory) {
                @Override
                public CompletableFuture<SendResult<String, String>> send(String topic, String data) {
                    return CompletableFuture.completedFuture(null);
                }
                
                @Override
                public CompletableFuture<SendResult<String, String>> send(String topic, String key, String data) {
                    return CompletableFuture.completedFuture(null);
                }
                
                @Override
                public CompletableFuture<SendResult<String, String>> send(ProducerRecord<String, String> record) {
                    return CompletableFuture.completedFuture(null);
                }
            };
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateOrder_Valid() throws Exception {
        OrderRequest req = new OrderRequest();
        req.setOrderId("order-1");
        req.setCustomerId("cust-1");
        req.setAmount(100.0);
        req.setTimestamp(System.currentTimeMillis());
        req.setItems(List.of("item1", "item2"));

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateOrder_Invalid() throws Exception {
        OrderRequest req = new OrderRequest();
        req.setOrderId(""); // Invalid
        req.setCustomerId(""); // Invalid
        req.setAmount(-1.0); // Invalid
        req.setTimestamp(0L); // Invalid
        req.setItems(List.of()); // Invalid

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_KafkaError() throws Exception {
        OrderRequest req = new OrderRequest();
        req.setOrderId("order-2");
        req.setCustomerId("cust-2");
        req.setAmount(200.0);
        req.setTimestamp(System.currentTimeMillis());
        req.setItems(List.of("item3"));

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
} 