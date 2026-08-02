package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    // One active cart per user, used to find-or-create on add-to-cart.
    Optional<Cart> findByUserId(UUID userId);
}
