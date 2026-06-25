package com.example.basketservice.basket.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BasketItemResponse(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
