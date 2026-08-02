package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.entity.StockReservation;
import com.ecommerce.catalog.entity.enums.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByOrderId(UUID orderId);

    // This method is defined now alongside the entity it queries, even though the expiry scheduler is not built yet.
    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);
}
