package com.dhobigo.backend.repository;

import com.dhobigo.backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
    List<Review> findByDhobiIdOrderByCreatedAtDesc(Long dhobiId);
    List<Review> findAllByOrderByCreatedAtDesc();
}
