package com.ecommerce.order.service;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.Payment;
import com.ecommerce.order.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// MOCK payment gateway integration for local development and demonstration purposes.
// This service simulates a payment gateway's behavior (success/failure) without making
// any real external network call. A real integration (Razorpay test mode, per our platform
// design) would replace the body of processPayment() with an actual HTTP call to the
// gateway's API, but the method's signature and surrounding transactional/persistence logic
// would stay the same — this mock exists so the rest of the saga (order status transitions,
// event publishing) can be built and tested end-to-end without needing real payment gateway
// credentials.
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment processPayment(Order order) {
        Payment payment = new Payment(order, order.getTotalCents());
        payment = paymentRepository.save(payment);

        // A real implementation would call out to the gateway here and branch on its
        // response, calling markSucceeded(...) or markFailed(...) accordingly. This mock
        // always succeeds so the full saga's happy path (order created -> stock reserved ->
        // payment succeeded -> order confirmed) can be exercised and verified end-to-end,
        // with failure-path testing possible later by temporarily changing this one line.
        payment.markSucceeded("mock_" + UUID.randomUUID());

        return payment;
    }
}
