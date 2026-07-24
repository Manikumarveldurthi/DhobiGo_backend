package com.dhobigo.backend.service;

import com.dhobigo.backend.dto.SubscriptionDtos.CreateSubscriptionRequest;
import com.dhobigo.backend.dto.SubscriptionDtos.SubscriptionResponse;
import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.Subscription;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.repository.SubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public SubscriptionResponse create(User customer, CreateSubscriptionRequest req) {
        Subscription sub = Subscription.builder()
                .customer(customer)
                .service(req.service())
                .frequency(req.frequency())
                .pickupSlot(req.pickupSlot())
                .pickupAddress(req.pickupAddress())
                .active(true)
                .nextRunDate(LocalDate.now().plusDays(req.frequency().days()))
                .build();
        return toResponse(subscriptionRepository.save(sub));
    }

    public List<SubscriptionResponse> myPlans(User customer) {
        return subscriptionRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SubscriptionResponse setActive(Long id, User customer, boolean active) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ApiException("Subscription not found", HttpStatus.NOT_FOUND));
        if (!sub.getCustomer().getId().equals(customer.getId())) {
            throw new ApiException("You don't have access to this subscription", HttpStatus.FORBIDDEN);
        }
        sub.setActive(active);
        return toResponse(subscriptionRepository.save(sub));
    }

    /** Called once a plan's pickup has actually been ordered — pushes the next date forward. */
    @Transactional
    public void advance(Subscription sub) {
        sub.setNextRunDate(sub.getNextRunDate().plusDays(sub.getFrequency().days()));
        subscriptionRepository.save(sub);
    }

    public List<Subscription> findDueToday() {
        return subscriptionRepository.findByActiveTrueAndNextRunDateLessThanEqual(LocalDate.now());
    }

    private SubscriptionResponse toResponse(Subscription s) {
        boolean dueToday = s.isActive() && !s.getNextRunDate().isAfter(LocalDate.now());
        return new SubscriptionResponse(
                s.getId(), s.getService(), s.getFrequency(), s.getPickupSlot(), s.getPickupAddress(),
                s.isActive(), s.getNextRunDate(), dueToday
        );
    }
}
