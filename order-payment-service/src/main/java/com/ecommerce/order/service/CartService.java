package com.ecommerce.order.service;

import com.ecommerce.order.dto.request.AddCartItemRequest;
import com.ecommerce.order.dto.response.CartItemResponse;
import com.ecommerce.order.dto.response.CartResponse;
import com.ecommerce.order.entity.Cart;
import com.ecommerce.order.exception.CartNotFoundException;
import com.ecommerce.order.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    // unitPriceCents is passed in as a parameter here rather than looked up inside this method,
    // because looking up a product's current price requires calling catalog-inventory-service
    // (via gRPC, in our eventual full design) - that cross-service call belongs in the
    // controller or a dedicated integration layer, not buried inside CartService, keeping this
    // service focused purely on cart persistence logic.
    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request, long unitPriceCents) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId)));
        cart.addItem(request.productId(), request.qty(), unitPriceCents);
        return toCartResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));
        return toCartResponse(cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));
        // No explicit save() call is needed here since cart is a managed entity within this
        // transaction - Hibernate's dirty checking (plus orphanRemoval=true on Cart.items)
        // handles persisting the change automatically on commit.
        cart.clear();
    }

    private CartResponse toCartResponse(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getItems().stream()
                        .map(item -> new CartItemResponse(
                                item.getId(),
                                item.getProductId(),
                                item.getQty(),
                                item.getUnitPriceCents(),
                                item.lineTotalCents()))
                        .toList(),
                cart.totalCents()
        );
    }
}
