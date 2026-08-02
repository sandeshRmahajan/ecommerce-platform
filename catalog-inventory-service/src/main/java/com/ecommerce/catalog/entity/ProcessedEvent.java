package com.ecommerce.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Records which incoming Kafka event IDs have already been handled by this service, so a
 * {@code @KafkaListener} can check "have I seen this event before?" prior to doing any real
 * work. This protects against Kafka's at-least-once delivery guarantee occasionally redelivering
 * the same message and causing duplicate processing (e.g. double-confirming a payment).
 */
@Entity
@Table(name = "processed_events", schema = "catalog")
@Getter
@NoArgsConstructor
public class ProcessedEvent {

    // Deliberately NOT @GeneratedValue: this ID is the same ID as the incoming Kafka event we're
    // recording as processed, not a new independently-generated identifier.
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
    }
}
