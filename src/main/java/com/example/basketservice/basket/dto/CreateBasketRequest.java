package com.example.basketservice.basket.dto;

import jakarta.validation.constraints.Size;

/**
 * Optional payload when creating a basket. {@code customerId} is nullable in
 * this assignment; with authentication it would be derived from the principal
 * rather than supplied by the client.
 */
public record CreateBasketRequest(
        @Size(max = 100, message = "customerId must not exceed 100 characters")
        String customerId) {
}
