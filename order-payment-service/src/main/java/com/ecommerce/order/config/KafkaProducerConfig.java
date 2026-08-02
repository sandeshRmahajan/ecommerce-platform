package com.ecommerce.order.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Values sent through this template are always already-serialized JSON strings (built
        // via ObjectMapper before being stored in OutboxEvent), so StringSerializer is used here
        // rather than JsonSerializer - using JsonSerializer on an already-JSON String value would
        // double-encode it (wrapping the JSON string in additional quotes/escaping), corrupting
        // the message for any consumer.
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // "all" means the producer waits for acknowledgment from all in-sync replicas before
        // considering a send successful (not just the partition leader) - the strongest
        // durability guarantee Kafka's producer config offers, appropriate here since these
        // events (order created, payment succeeded) represent real business-critical state
        // changes we cannot afford to silently lose.
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        // Deduplicates retries at the Kafka client protocol level, complementing (not replacing)
        // the ProcessedEvent-based idempotency check on the consumer side, which protects
        // against duplicates from any cause, not just producer retries.
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
