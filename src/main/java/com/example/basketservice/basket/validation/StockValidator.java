package com.example.basketservice.basket.validation;

import com.example.basketservice.basket.entity.BasketItem;
import com.example.basketservice.common.exception.ResourceNotFoundException;
import com.example.basketservice.product.entity.Product;
import com.example.basketservice.product.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StockValidator {

    private final ProductRepository productRepository;

    public StockValidator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void validate(List<BasketItem> items) {
        for (BasketItem item : items) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.getProduct().getId()));

            product.ensureStockAvailable(item.getQuantity());
        }
    }
}