package com.dhobigo.backend.repository;

import com.dhobigo.backend.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Subscription> findByActiveTrueAndNextRunDateLessThanEqual(LocalDate date);
}
