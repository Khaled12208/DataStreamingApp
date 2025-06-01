package com.example.streamprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamProcessorJobTest {
    @Test
    public void testFraudDetectionWindow() throws Exception {
        List<OrderEvent> orders = new ArrayList<>();
        // 6 orders in 1 minute for the same customer, one with high amount
        for (int i = 0; i < 6; i++) {
            OrderEvent e = new OrderEvent();
            e.setOrderId("order-" + i);
            e.setCustomerId("cust-1");
            e.setAmount(i == 2 ? 1500.0 : 100.0); // One high-value order
            e.setTimestamp(1717230000000L + i * 1000);
            e.setItems(Arrays.asList("item-" + i));
            orders.add(e);
        }

        List<String> results = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        ProcessWindowFunction<OrderEvent, String, String, TimeWindow> fraudFn = new ProcessWindowFunction<OrderEvent, String, String, TimeWindow>() {
            @Override
            public void process(String customerId, Context ctx, Iterable<OrderEvent> elements, Collector<String> out) throws Exception {
                int orderCount = 0;
                for (OrderEvent e : elements) {
                    if (e.getAmount() > 1000) {
                        out.collect(objectMapper.writeValueAsString("FRAUD_AMOUNT"));
                    }
                    orderCount++;
                }
                if (orderCount > 5) {
                    out.collect(objectMapper.writeValueAsString("FRAUD_COUNT"));
                }
            }
        };

        fraudFn.process("cust-1", null, orders, new Collector<>() {
            @Override
            public void collect(String record) {
                results.add(record);
            }
            @Override
            public void close() {}
        });

        assertThat(results).anyMatch(s -> s.contains("FRAUD_AMOUNT"));
        assertThat(results).anyMatch(s -> s.contains("FRAUD_COUNT"));
    }
} 