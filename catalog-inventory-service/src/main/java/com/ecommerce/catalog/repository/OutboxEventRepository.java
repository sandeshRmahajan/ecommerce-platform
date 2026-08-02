package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    // Used by the future background publisher: unpublished events, oldest first, so events are
    // published in the order they were created.
    // findByPublishedFalse is a derived query on a boolean field (Spring Data recognizes the
    // PropertyFalse/PropertyTrue keywords as shorthand for "= false" / "= true"), and
    // OrderByCreatedAtAsc adds an ORDER BY clause directly from the method name - both are
    // standard Spring Data JPA derived query features, not something we're inventing.
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
}
