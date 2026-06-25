    package com.example.basketservice.basket.dto;

    import com.example.basketservice.basket.entity.BasketStatus;

    import java.math.BigDecimal;
    import java.time.Instant;
    import java.util.List;
    import java.util.UUID;

    public record BasketResponse(
            UUID id,
            String customerId,
            BasketStatus status,
            List<BasketItemResponse> items,
            BigDecimal totalAmount,
            Instant createdAt,
            Instant updatedAt) {
    }
