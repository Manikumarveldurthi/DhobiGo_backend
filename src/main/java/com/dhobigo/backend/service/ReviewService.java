package com.dhobigo.backend.service;

import com.dhobigo.backend.dto.ReviewDtos.CreateReviewRequest;
import com.dhobigo.backend.dto.ReviewDtos.ReviewResponse;
import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.*;
import com.dhobigo.backend.repository.DhobiProfileRepository;
import com.dhobigo.backend.repository.OrderRepository;
import com.dhobigo.backend.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final DhobiProfileRepository dhobiProfileRepository;

    public ReviewService(ReviewRepository reviewRepository, OrderRepository orderRepository, DhobiProfileRepository dhobiProfileRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.dhobiProfileRepository = dhobiProfileRepository;
    }

    @Transactional
    public ReviewResponse create(User customer, Long orderId, CreateReviewRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ApiException("You can only review your own orders", HttpStatus.FORBIDDEN);
        }
        if (order.getStage() != OrderStage.DELIVERED) {
            throw new ApiException("You can only review an order after it's delivered", HttpStatus.BAD_REQUEST);
        }
        if (order.getDhobi() == null) {
            throw new ApiException("This order has no assigned dhobi to review", HttpStatus.BAD_REQUEST);
        }
        if (reviewRepository.existsByOrderId(orderId)) {
            throw new ApiException("You've already reviewed this order", HttpStatus.CONFLICT);
        }

        Review review = reviewRepository.save(
                Review.builder()
                        .order(order)
                        .customer(customer)
                        .dhobi(order.getDhobi())
                        .rating(req.rating())
                        .comment(req.comment())
                        .build()
        );

        recomputeDhobiRating(order.getDhobi().getId());

        return toResponse(review);
    }

    public Optional<ReviewResponse> getForOrder(Long orderId) {
        return reviewRepository.findByOrderId(orderId).map(this::toResponse);
    }

    public List<ReviewResponse> getForDhobi(Long dhobiId) {
        return reviewRepository.findByDhobiIdOrderByCreatedAtDesc(dhobiId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReviewResponse> getAll() {
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    /** Keeps DhobiProfile.rating as a true average of all reviews — this is
        what feeds the "Top Rated" badge and nearby-dhobi sort, so ratings
        are always real once reviews start coming in (not just the seeded
        starting value). */
    private void recomputeDhobiRating(Long dhobiId) {
        List<Review> reviews = reviewRepository.findByDhobiIdOrderByCreatedAtDesc(dhobiId);
        if (reviews.isEmpty()) return;

        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(5.0);
        dhobiProfileRepository.findByUserId(dhobiId).ifPresent(profile -> {
            profile.setRating(Math.round(avg * 10.0) / 10.0); // one decimal place
            dhobiProfileRepository.save(profile);
        });
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getOrder().getId(),
                r.getOrder().getOrderCode(),
                r.getCustomer().getId(),
                r.getCustomer().getFullName(),
                r.getDhobi().getId(),
                r.getDhobi().getFullName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
