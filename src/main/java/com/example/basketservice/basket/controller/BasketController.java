package com.example.basketservice.basket.controller;

import com.example.basketservice.basket.dto.*;
import com.example.basketservice.basket.service.BasketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/baskets")
@Tag(name = "Baskets", description = "Create and manage a customer's basket")
public class BasketController {

    private final BasketService basketService;

    public BasketController(BasketService basketService) {
        this.basketService = basketService;
    }

    @PostMapping
    @Operation(summary = "Create a new basket")
    public ResponseEntity<BasketResponse> createBasket(
            @RequestBody(required = false) CreateBasketRequest request) {

        BasketResponse created = basketService.createBasket(request);
        return ResponseEntity
                .created(URI.create("/api/v1/baskets/" + created.id()))
                .body(created);
    }

    @GetMapping("/{basketId}")
    @Operation(summary = "Get a basket and its current contents")
    public ResponseEntity<BasketResponse> getBasket(@PathVariable UUID basketId) {

        return ResponseEntity.ok(
                basketService.getBasket(basketId)
        );
    }

    @PostMapping("/{basketId}/items")
    @Operation(summary = "Add a product to the basket (increments existing quantity)")
    public ResponseEntity<BasketResponse> addItem(
            @PathVariable UUID basketId,
            @Valid @RequestBody AddItemRequest request) {

        return ResponseEntity.ok(
                basketService.addItem(basketId, request)
        );
    }

    @PutMapping("/{basketId}/items/{productId}")
    @Operation(summary = "Set the absolute quantity of a product in the basket")
    public ResponseEntity<BasketResponse> updateItem(
            @PathVariable UUID basketId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateItemRequest request) {

        return ResponseEntity.ok(
                basketService.updateItem(basketId, productId, request.quantity())
        );
    }

    @DeleteMapping("/{basketId}/items/{productId}")
    @Operation(summary = "Remove a product from the basket")
    public ResponseEntity<Void> removeItem(
            @PathVariable UUID basketId,
            @PathVariable UUID productId) {

        basketService.removeItem(basketId, productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{basketId}/checkout")
    @Operation(summary = "Finalise the basket and return the priced checkout summary")
    public ResponseEntity<CheckoutResponse> checkout(@PathVariable UUID basketId) {

        return ResponseEntity.ok(
                basketService.checkout(basketId)
        );
    }
}