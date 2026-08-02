package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Look up the payment attempt tied to a given order. Derived via the "order" relationship's
    // id property (Payment.order -> Order.id), since Payment has no direct orderId field.
    Optional<Payment> findByOrderId(UUID orderId);
}
