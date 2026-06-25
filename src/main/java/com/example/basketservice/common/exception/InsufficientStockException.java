package com.example.basketservice.common.exception;

import java.util.UUID;

/**
 * Thrown when the requested quantity exceeds the available warehouse stock.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(UUID id, String name, int requestedQuantity, int quantity) {
        super("Insufficient stock for product %s ('%s'): requested %d, available %d"
                .formatted(id, name, requestedQuantity, quantity));
    }
}