package com.example.basketservice.basket.entity;

/**
 * Lifecycle of a basket.
 *
 * <p>ACTIVE baskets are mutable. Once CHECKED_OUT, a basket is frozen so its
 * priced contents cannot change underneath an in-flight checkout/order.
 */
public enum BasketStatus {
    ACTIVE,
    CHECKED_OUT
}
