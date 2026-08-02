package com.ecommerce.order.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, String> consumerFactory(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // The consumer group id determines which consumers share the work of reading a topic's
        // partitions. Using the service's own name as the group id is a standard convention so
        // it's immediately clear from Kafka's own tooling which service is consuming what.
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "order-payment-service");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // We deserialize the raw value as a String here (not directly to a specific event type),
        // then parse that JSON string manually inside the listener method using ObjectMapper -
        // this mirrors the producer side, which always sends pre-serialized JSON strings, and
        // keeps the deserialization step explicit and controlled in application code rather than
        // relying on Spring Kafka's automatic type-mapping machinery, which is harder to reason
        // about when a single listener may need to distinguish between multiple possible event
        // shapes.
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // On first startup with no prior committed offset for this consumer group, start reading
        // from the beginning of the topic rather than only new messages going forward -
        // reasonable for local development so a freshly-started consumer doesn't miss events
        // published slightly before it came up.
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
