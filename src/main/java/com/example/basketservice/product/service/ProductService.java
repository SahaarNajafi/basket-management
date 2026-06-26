package com.example.basketservice.product.service;

import com.example.basketservice.common.dto.PagedResponse;
import com.example.basketservice.common.exception.ResourceNotFoundException;
import com.example.basketservice.product.dto.ProductResponse;
import com.example.basketservice.product.mapper.ProductMapper;
import com.example.basketservice.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;


    public ProductService(ProductRepository productRepository,
                          ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public PagedResponse<ProductResponse> listProducts(Pageable pageable) {
        LOGGER.debug("Listing paginated products with settings: {}", pageable);
        return PagedResponse.from(productRepository.findAll(pageable).map(productMapper::toResponse));
    }

    public ProductResponse getProduct(UUID productId) {
        LOGGER.debug("Retrieving details for product ID: {}", productId);
        return productRepository.findById(productId)
                .map(productMapper::toResponse)
                .orElseThrow(() -> {
                    LOGGER.warn("Product with ID {} not found", productId);
                    return new ResourceNotFoundException("Product", productId);
                });
    }

}
