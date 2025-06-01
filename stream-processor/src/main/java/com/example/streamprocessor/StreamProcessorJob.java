package com.example.streamprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.flink.api.common.ExecutionConfig;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StreamProcessorJob {
    private static final Logger logger = LoggerFactory.getLogger(StreamProcessorJob.class);

    public static void main(String[] args) throws Exception {
        logger.info("Starting Stream Processor Job");
        try {
            final StreamExecutionEnvironment env = createStreamExecutionEnvironment();
            
            // Configure restart strategy
            env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                3, // number of restart attempts
                10000 // delay between attempts in milliseconds
            ));
            
            // Enable checkpointing for exactly-once processing
            // env.enableCheckpointing(60000); // Checkpoint every minute - disabled temporarily due to volume permission issues
            
            final ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            String kafkaBootstrap = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
            String inputTopic = "orders";
            String outputTopic = "analytics";

            KafkaSource<String> source = KafkaSource.<String>builder()
                    .setBootstrapServers(kafkaBootstrap)
                    .setTopics(inputTopic)
                    .setGroupId("flink-stream-processor")
                    .setStartingOffsets(OffsetsInitializer.earliest())
                    .setValueOnlyDeserializer(new SimpleStringSchema())
                    .build();

            // Use RichMapFunction for better serialization handling
            class OrderEventDeserializer extends RichMapFunction<String, OrderEvent> {
                private static final long serialVersionUID = 1L;
                private transient ObjectMapper mapper;
                
                @Override
                public void open(org.apache.flink.configuration.Configuration parameters) {
                    mapper = new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                }
                
                @Override
                public OrderEvent map(String json) throws Exception {
                    return mapper.readValue(json, OrderEvent.class);
                }
            }

            DataStream<OrderEvent> orders = env.fromSource(
                    source,
                    WatermarkStrategy.noWatermarks(),
                    "KafkaSource-orders")
                .map(new OrderEventDeserializer())
                .returns(TypeInformation.of(OrderEvent.class));

            // Hourly sales and average order value
            DataStream<String> hourlySales = orders
                .assignTimestampsAndWatermarks(WatermarkStrategy.<OrderEvent>forMonotonousTimestamps()
                    .withTimestampAssigner((event, ts) -> event.getTimestamp()))
                .windowAll(TumblingEventTimeWindows.of(Time.hours(1)))
                .process(new ProcessAllWindowFunction<OrderEvent, String, TimeWindow>() {
                    @Override
                    public void process(Context ctx, Iterable<OrderEvent> elements, Collector<String> out) throws Exception {
                        double totalSales = 0;
                        int count = 0;
                        for (OrderEvent e : elements) {
                            totalSales += e.getAmount();
                            count++;
                        }
                        double avgOrderValue = count > 0 ? totalSales / count : 0;
                        String windowStart = Instant.ofEpochMilli(ctx.window().getStart()).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        Map<String, Object> result = new HashMap<>();
                        result.put("type", "hourly_sales");
                        result.put("window_start", windowStart);
                        result.put("total_sales", totalSales);
                        result.put("average_order_value", avgOrderValue);
                        out.collect(objectMapper.writeValueAsString(result));
                    }
                });

            // Orders per customer per hour
            DataStream<String> customerStats = orders
                .assignTimestampsAndWatermarks(WatermarkStrategy.<OrderEvent>forMonotonousTimestamps()
                    .withTimestampAssigner((event, ts) -> event.getTimestamp()))
                .keyBy(OrderEvent::getCustomerId)
                .window(TumblingEventTimeWindows.of(Time.hours(1)))
                .process(new ProcessWindowFunction<OrderEvent, String, String, TimeWindow>() {
                    @Override
                    public void process(String customerId, Context ctx, Iterable<OrderEvent> elements, Collector<String> out) throws Exception {
                        int orderCount = 0;
                        double total = 0;
                        for (OrderEvent e : elements) {
                            orderCount++;
                            total += e.getAmount();
                        }
                        String windowStart = Instant.ofEpochMilli(ctx.window().getStart()).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        Map<String, Object> result = new HashMap<>();
                        result.put("type", "customer_stats");
                        result.put("window_start", windowStart);
                        result.put("customer_id", customerId);
                        result.put("orders", orderCount);
                        result.put("total_spent", total);
                        out.collect(objectMapper.writeValueAsString(result));
                    }
                });

            // Fraud detection: amount > $1000 or > 5 orders/minute per customer
            DataStream<String> fraudAlerts = orders
                .assignTimestampsAndWatermarks(WatermarkStrategy.<OrderEvent>forMonotonousTimestamps()
                    .withTimestampAssigner((event, ts) -> event.getTimestamp()))
                .keyBy(OrderEvent::getCustomerId)
                .window(TumblingEventTimeWindows.of(Time.minutes(1)))
                .process(new ProcessWindowFunction<OrderEvent, String, String, TimeWindow>() {
                    @Override
                    public void process(String customerId, Context ctx, Iterable<OrderEvent> elements, Collector<String> out) throws Exception {
                        int orderCount = 0;
                        for (OrderEvent e : elements) {
                            if (e.getAmount() > 1000) {
                                Map<String, Object> alert = new HashMap<>();
                                alert.put("type", "fraud_alert");
                                alert.put("customer_id", customerId);
                                alert.put("reason", "Order amount > $1000");
                                alert.put("order_id", e.getOrderId());
                                alert.put("amount", e.getAmount());
                                out.collect(objectMapper.writeValueAsString(alert));
                            }
                            orderCount++;
                        }
                        if (orderCount > 5) {
                            Map<String, Object> alert = new HashMap<>();
                            alert.put("type", "fraud_alert");
                            alert.put("customer_id", customerId);
                            alert.put("reason", ">5 orders per minute");
                            alert.put("order_count", orderCount);
                            out.collect(objectMapper.writeValueAsString(alert));
                        }
                    }
                });

            // Union all analytics streams
            DataStream<String> analytics = hourlySales.union(customerStats, fraudAlerts);

            // Sink to analytics topic
            KafkaSink<String> sink = KafkaSink.<String>builder()
                    .setBootstrapServers(kafkaBootstrap)
                    .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                            .setTopic(outputTopic)
                            .setValueSerializationSchema(new SimpleStringSchema())
                            .build())
                    .build();

            analytics.sinkTo(sink);

            logger.info("Starting Flink job execution");
            env.execute("Order Processing Job");
        } catch (Exception e) {
            logger.error("Error in Stream Processor Job", e);
            throw e;
        }
    }

    private static StreamExecutionEnvironment createStreamExecutionEnvironment() {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Configure Kryo serializer
        env.getConfig().enableForceKryo();
        
        // Register Kryo serializers
        env.getConfig().registerTypeWithKryoSerializer(OrderEvent.class, OrderEventSerializer.class);
        
        // Add Java module opens
        System.setProperty("java.lang.invoke.stringConcat", "BC_SB");
        System.setProperty("jdk.module.illegalAccess.permit", "true");
        
        return env;
    }
    
    // Custom serializer for OrderEvent
    public static class OrderEventSerializer extends com.esotericsoftware.kryo.Serializer<OrderEvent> {
        @Override
        public void write(Kryo kryo, Output output, OrderEvent object) {
            output.writeString(object.getOrderId());
            output.writeString(object.getCustomerId());
            output.writeDouble(object.getAmount());
            kryo.writeObjectOrNull(output, object.getTimestamp(), Long.class);
            kryo.writeObjectOrNull(output, object.getItems(), ArrayList.class);
        }

        @Override
        public OrderEvent read(Kryo kryo, Input input, Class<OrderEvent> type) {
            OrderEvent event = new OrderEvent();
            event.setOrderId(input.readString());
            event.setCustomerId(input.readString());
            event.setAmount(input.readDouble());
            event.setTimestamp(kryo.readObjectOrNull(input, Long.class));
            event.setItems(kryo.readObjectOrNull(input, ArrayList.class));
            return event;
        }
    }
} 