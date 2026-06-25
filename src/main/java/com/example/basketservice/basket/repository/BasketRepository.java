package com.example.basketservice.basket.repository;

import com.example.basketservice.basket.entity.Basket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BasketRepository extends JpaRepository<Basket, UUID> {

    /**
     * Loads a basket together with its items and each item's product in a single
     * query, so mapping to the response does not trigger N+1 lazy loads.
     */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Basket> findWithItemsById(UUID id);
}