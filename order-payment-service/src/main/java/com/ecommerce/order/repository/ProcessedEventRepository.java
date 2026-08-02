package com.ecommerce.order.repository;

import com.ecommerce.order.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// JpaRepository.existsById(UUID) is already sufficient for the "have we already processed this
// event?" check used by Kafka listeners, so this repository intentionally has no custom methods
// beyond what JpaRepository already provides.
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
