package com.ecommerce.catalog.outbox;

import com.ecommerce.catalog.entity.OutboxEvent;
import com.ecommerce.catalog.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// This is the second half of the transactional outbox pattern: CatalogService writes events into
// the outbox_events table transactionally alongside business data, and this class is solely
// responsible for actually getting those events onto Kafka. It polls the table on a fixed
// interval for unpublished events, sends each one to Kafka using its eventType as the topic
// name, and marks it published only after a successful send.
//
// Annotated @Component rather than @Service because this is infrastructure/scheduling machinery,
// not core business logic.
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();
        // Early return avoids unnecessary logging/work on every single poll when there's nothing
        // to do, which will be the common case most of the time.
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                // Arguments: topic name (the event type string, e.g. "order.created"), the
                // message key (the aggregate's own id as a string - using a consistent key per
                // aggregate ensures Kafka routes all events about the same order to the same
                // partition, preserving relative ordering for that specific order across multiple
                // events), and the value (the already-JSON-serialized payload string built
                // earlier in OrderService).
                // .get() blocks until the send actually completes (or throws if it failed), converting
                // KafkaTemplate's asynchronous CompletableFuture-based API into a synchronous call here.
                // This is deliberate: without blocking on the result, send() returns immediately after only
                // QUEUING the message, and markPublished() below would incorrectly run even if the send later
                // fails once actually attempted against the broker (e.g. broker unreachable) - silently
                // defeating the outbox pattern's entire reliability guarantee. Blocking here inside a
                // scheduled background task (not a user-facing request) is an acceptable tradeoff since
                // nothing else is waiting on this thread.
                kafkaTemplate.send(event.getEventType(), event.getAggregateId().toString(), event.getPayload()).get();
                // Relies on this @Transactional method's dirty-checking to persist the change,
                // same pattern as Cart.clear()/CartService.clearCart.
                event.markPublished();
            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                if (e instanceof InterruptedException) {
                    // Re-interrupting the thread here preserves the interrupt status for any surrounding
                    // framework code (like Spring's scheduler) that may also need to observe it, since
                    // catching InterruptedException clears the flag by default, which could otherwise
                    // cause the interrupt signal to be silently lost.
                    Thread.currentThread().interrupt();
                }
                // A single failed send must not prevent other pending events from being
                // attempted. The failed event simply remains unpublished (published stays false)
                // to be retried automatically on the next scheduled run.
                log.error("Failed to publish outbox event id={} eventType={}", event.getId(), event.getEventType(), e);
            }
        }
    }
}
