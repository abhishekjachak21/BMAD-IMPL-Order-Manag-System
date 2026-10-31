package com.oms.order_management_system.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query("select p from Payment p where p.order.id = :orderId and p.status in :statuses order by p.createdAt desc")
    List<Payment> findByOrderIdAndStatusIn(@Param("orderId") UUID orderId,
                                          @Param("statuses") List<PaymentStatus> statuses);

    @Query("select p from Payment p where p.order.id = :orderId order by p.createdAt desc")
    List<Payment> findByOrderIdOrderByCreatedAtDesc(@Param("orderId") UUID orderId);
}
