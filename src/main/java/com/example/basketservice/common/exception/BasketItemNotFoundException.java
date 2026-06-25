package com.example.basketservice.common.exception;

import java.util.UUID;

public class BasketItemNotFoundException extends RuntimeException {

    public BasketItemNotFoundException(UUID basketId, UUID productId) {
        super("Product %s was not found in basket %s"
                .formatted(productId, basketId));
    }
}