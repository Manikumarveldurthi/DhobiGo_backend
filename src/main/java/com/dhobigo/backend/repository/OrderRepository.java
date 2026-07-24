package com.dhobigo.backend.repository;

import com.dhobigo.backend.model.Order;
import com.dhobigo.backend.model.OrderStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Order> findByDhobiIdOrderByCreatedAtDesc(Long dhobiId);
    List<Order> findAllByOrderByCreatedAtDesc();
    boolean existsByCustomerIdAndDhobiId(Long customerId, Long dhobiId);

    /** Used to compute a customer's loyalty tier (Bronze/Silver/Gold) in UserController. */
    long countByCustomerIdAndStage(Long customerId, OrderStage stage);
}
