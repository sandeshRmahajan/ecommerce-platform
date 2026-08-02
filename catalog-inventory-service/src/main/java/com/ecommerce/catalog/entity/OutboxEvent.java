package com.ecommerce.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", schema = "catalog")
@Getter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Identifies which kind of business entity this event is about, e.g. "Order" or "Payment" —
    // useful for filtering/debugging the outbox table without needing to inspect the JSON payload.
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    // The actual Order/Payment id this event relates to.
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    // The Kafka topic-style event name, e.g. "order.created", matching the naming convention
    // from our platform design doc.
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    // Stores the already-serialized event body as a JSON string (e.g. via ObjectMapper in the
    // service layer before constructing this entity), rather than a strongly-typed record like
    // Order.shippingAddress, because outbox_events intentionally stores MANY DIFFERENT event
    // shapes (order.created, payment.succeeded, etc.) in the same table/column, so a single
    // fixed Java type would not fit every event type. The tradeoff is we lose compile-time type
    // safety on this specific field, which is an accepted cost of a generic outbox table design.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false)
    private boolean published;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OutboxEvent(String aggregateType, UUID aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.published = false;
    }

    // Called by the background publisher process (to be built later) once the event has been
    // successfully sent to Kafka.
    public void markPublished() {
        this.published = true;
    }
}
