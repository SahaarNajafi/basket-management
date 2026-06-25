package com.example.basketservice.product.mapper;

import com.example.basketservice.product.dto.ProductResponse;
import com.example.basketservice.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getAvailableQuantity()
        );
    }

}
