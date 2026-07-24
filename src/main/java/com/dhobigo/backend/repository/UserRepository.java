package com.dhobigo.backend.repository;

import com.dhobigo.backend.model.Role;
import com.dhobigo.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    List<User> findByRole(Role role);
    Optional<User> findByReferralCode(String referralCode);
    boolean existsByReferralCode(String referralCode);
}
