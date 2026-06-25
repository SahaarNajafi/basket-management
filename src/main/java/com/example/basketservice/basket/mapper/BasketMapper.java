package com.example.basketservice.basket.mapper;

import com.example.basketservice.basket.dto.BasketItemResponse;
import com.example.basketservice.basket.dto.BasketResponse;
import com.example.basketservice.basket.dto.CheckoutResponse;
import com.example.basketservice.basket.entity.Basket;
import com.example.basketservice.basket.entity.BasketItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Maps basket entities to API response models and applies monetary scaling.
 *
 * <p>All amounts are normalised to 2 decimal places with HALF_UP rounding at the
 * boundary so clients receive consistent currency values.
 */
@Component
public class BasketMapper {

    private static final int MONEY_SCALE = 2;

    public BasketResponse toBasketResponse(Basket basket) {
        return new BasketResponse(
                basket.getId(),
                basket.getCustomerId(),
                basket.getStatus(),
                mapToItemResponses(basket.getItems()),
                scale(basket.getTotalAmount()),
                basket.getCreatedAt(),
                basket.getUpdatedAt());
    }

    public CheckoutResponse toCheckoutResponse(Basket basket) {
        int totalItems = basket.getItems().stream()
                .mapToInt(BasketItem::getQuantity)
                .sum();

        return new CheckoutResponse(
                basket.getId(),
                basket.getStatus(),
                mapToItemResponses(basket.getItems()),
                totalItems,
                scale(basket.getTotalAmount()));
    }

    // Single source of truth for converting the list
    private List<BasketItemResponse> mapToItemResponses(List<BasketItem> items) {
        return items.stream()
                .map(this::toItemResponse)
                .toList();
    }

    private BasketItemResponse toItemResponse(BasketItem item) {
        return new BasketItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                scale(item.getUnitPrice()),
                scale(item.getLineTotal()));
    }

    private BigDecimal scale(BigDecimal value) {
        // Handle nulls gracefully if they can occur, otherwise this is fine
        return value == null ? BigDecimal.ZERO.setScale(MONEY_SCALE) : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}

