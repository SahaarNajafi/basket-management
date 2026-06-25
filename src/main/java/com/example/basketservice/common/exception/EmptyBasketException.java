package com.example.basketservice.common.exception;

import java.util.UUID;

/** Thrown when attempting to check out a basket that contains no items. */
public class EmptyBasketException extends RuntimeException {

    public EmptyBasketException(UUID basketId) {
        super("Basket %s is empty".formatted(basketId));
    }
}
