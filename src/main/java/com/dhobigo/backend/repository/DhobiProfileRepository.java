package com.dhobigo.backend.repository;

import com.dhobigo.backend.model.DhobiProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DhobiProfileRepository extends JpaRepository<DhobiProfile, Long> {
    Optional<DhobiProfile> findByUserId(Long userId);

    /** Live/bookable dhobis — shown to customers, eligible for auto-assignment. */
    List<DhobiProfile> findByApprovedTrueAndAvailableTrue();

    /** Awaiting admin review — powers the admin approval queue. */
    List<DhobiProfile> findByApprovedFalse();
}
