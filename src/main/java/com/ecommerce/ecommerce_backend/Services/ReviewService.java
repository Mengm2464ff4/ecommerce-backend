package com.ecommerce.ecommerce_backend.Services;

import com.ecommerce.ecommerce_backend.dto.request.ReviewRequest;
import com.ecommerce.ecommerce_backend.dto.response.ReviewResponse;
import com.ecommerce.ecommerce_backend.exception.CustomException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.model.Product;
import com.ecommerce.ecommerce_backend.model.Review;
import com.ecommerce.ecommerce_backend.model.User;
import com.ecommerce.ecommerce_backend.Repositories.ProductRepository;
import com.ecommerce.ecommerce_backend.Repositories.ReviewRepository;
import com.ecommerce.ecommerce_backend.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse createReview(String username, ReviewRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        // Check if user already reviewed this product
        if (reviewRepository.existsByProductIdAndUserId(product.getId(), user.getId())) {
            throw CustomException.conflict("You have already reviewed this product");
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("Review created by {} for product {}", username, product.getName());

        return convertToResponse(savedReview);
    }

    public Page<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable) {
        // Check product exists
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        return reviewRepository.findByProductId(productId, pageable)
                .map(this::convertToResponse);
    }

    public ReviewResponse getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        return convertToResponse(review);
    }

    @Transactional
    public ReviewResponse updateReview(String username, Long reviewId, ReviewRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        // Only the owner can update
        if (!review.getUser().getId().equals(user.getId())) {
            throw CustomException.forbidden("You can only update your own review");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review updatedReview = reviewRepository.save(review);
        log.info("Review {} updated by {}", reviewId, username);

        return convertToResponse(updatedReview);
    }

    @Transactional
    public void deleteReview(String username, Long reviewId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        // Only the owner can delete (you can also allow ADMIN later)
        if (!review.getUser().getId().equals(user.getId())) {
            throw CustomException.forbidden("You can only delete your own review");
        }

        reviewRepository.delete(review);
        log.info("Review {} deleted by {}", reviewId, username);
    }

    public Double getAverageRating(Long productId) {
        Double average = reviewRepository.findAverageRatingByProductId(productId);
        return average != null ? average : 0.0;
    }

    public long getReviewCount(Long productId) {
        return reviewRepository.countByProductId(productId);
    }

    private ReviewResponse convertToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getId())
                .username(review.getUser().getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}