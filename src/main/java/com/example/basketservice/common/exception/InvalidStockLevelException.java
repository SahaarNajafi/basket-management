package com.example.basketservice.common.exception;

public class InvalidStockLevelException extends RuntimeException {

    public InvalidStockLevelException(int stock) {
        super("Stock level cannot be negative: " + stock);
    }
}