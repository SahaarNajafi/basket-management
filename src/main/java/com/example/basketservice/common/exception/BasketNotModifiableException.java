package com.example.basketservice.common.exception;

import com.example.basketservice.basket.entity.BasketStatus;

import java.util.UUID;

/** Thrown when attempting to modify a basket that is no longer ACTIVE. */
public class BasketNotModifiableException extends RuntimeException {

    public BasketNotModifiableException(
            UUID basketId,
            BasketStatus status
    ) {
        super("Basket %s is %s and cannot be modified"
                .formatted(basketId, status));
    }
}