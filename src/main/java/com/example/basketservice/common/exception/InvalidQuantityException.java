package com.example.basketservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested quantity is invalid (e.g., zero or negative).
 */
public class InvalidQuantityException extends RuntimeException {

    public InvalidQuantityException(int quantity) {
        super("Quantity must be greater than zero: " + quantity);
    }
}