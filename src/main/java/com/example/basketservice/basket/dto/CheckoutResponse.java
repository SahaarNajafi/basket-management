package com.example.basketservice.basket.dto;

import com.example.basketservice.basket.entity.BasketStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The priced summary returned when a basket is finalised for checkout. Payment
 * processing itself is out of scope and handled by a separate order service.
 */
public record CheckoutResponse(
        UUID basketId,
        BasketStatus status,
        List<BasketItemResponse> items,
        int totalItems,
        BigDecimal totalAmount) {
}
