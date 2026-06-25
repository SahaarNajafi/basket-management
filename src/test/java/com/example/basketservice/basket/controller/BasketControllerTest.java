package com.example.basketservice.basket.controller;

import com.example.basketservice.basket.dto.*;
import com.example.basketservice.basket.service.BasketService;
import com.example.basketservice.basket.entity.BasketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasketControllerTest {

    @Mock
    private BasketService basketService;

    @InjectMocks
    private BasketController basketController;

    @Test
    void createBasket_shouldReturnCreatedBasket() {
        CreateBasketRequest request = new CreateBasketRequest("customer-1");

        BasketResponse response = new BasketResponse(
                UUID.randomUUID(),
                "customer-1",
                BasketStatus.ACTIVE,
                Collections.emptyList(),
                BigDecimal.ZERO,
                null,
                null
        );

        when(basketService.createBasket(request)).thenReturn(response);

        ResponseEntity<BasketResponse> result = basketController.createBasket(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void getBasket_shouldReturnBasket() {
        UUID basketId = UUID.randomUUID();

        BasketResponse response = mock(BasketResponse.class);

        when(basketService.getBasket(basketId)).thenReturn(response);

        ResponseEntity<BasketResponse> result = basketController.getBasket(basketId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void addItem_shouldReturnUpdatedBasket() {
        UUID basketId = UUID.randomUUID();
        AddItemRequest request = new AddItemRequest(UUID.randomUUID(), 2);

        BasketResponse response = mock(BasketResponse.class);

        when(basketService.addItem(basketId, request)).thenReturn(response);

        ResponseEntity<BasketResponse> result = basketController.addItem(basketId, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void updateItem_shouldReturnUpdatedBasket() {
        UUID basketId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UpdateItemRequest request = new UpdateItemRequest(3);

        BasketResponse response = mock(BasketResponse.class);

        when(basketService.updateItem(basketId, productId, request.quantity()))
                .thenReturn(response);

        ResponseEntity<BasketResponse> result =
                basketController.updateItem(basketId, productId, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void removeItem_shouldReturn204() {
        UUID basketId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(basketService.removeItem(basketId, productId))
                .thenReturn(mock(BasketResponse.class));

        ResponseEntity<Void> result =
                basketController.removeItem(basketId, productId);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void checkout_shouldReturnCheckoutResponse() {
        UUID basketId = UUID.randomUUID();

        CheckoutResponse response = mock(CheckoutResponse.class);

        when(basketService.checkout(basketId)).thenReturn(response);

        ResponseEntity<CheckoutResponse> result =
                basketController.checkout(basketId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }
}