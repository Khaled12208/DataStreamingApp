package com.example.streamprocessor;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;

public class OrderEventTest {
    @Test
    public void testGettersSetters() {
        OrderEvent event = new OrderEvent();
        event.setOrderId("order-1");
        event.setCustomerId("cust-1");
        event.setAmount(123.45);
        event.setTimestamp(1717230000000L);
        event.setItems(Arrays.asList("item1", "item2"));

        assertThat(event.getOrderId()).isEqualTo("order-1");
        assertThat(event.getCustomerId()).isEqualTo("cust-1");
        assertThat(event.getAmount()).isEqualTo(123.45);
        assertThat(event.getTimestamp()).isEqualTo(1717230000000L);
        assertThat(event.getItems()).containsExactly("item1", "item2");
    }
} 