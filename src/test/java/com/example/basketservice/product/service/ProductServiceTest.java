package com.example.basketservice.product.service;

import com.example.basketservice.common.exception.ResourceNotFoundException;
import com.example.basketservice.product.dto.ProductResponse;
import com.example.basketservice.product.entity.Product;
import com.example.basketservice.product.mapper.ProductMapper;
import com.example.basketservice.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductMapper productMapper;
    @InjectMocks private ProductService productService;

    @Test
    void getProduct_Success() {
        UUID id = UUID.randomUUID();
        Product product = new Product("Test Product", new BigDecimal("89.99"), 10);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(new ProductResponse(id, "Test Product", new BigDecimal("89.99"), 10));

        ProductResponse response = productService.getProduct(id);

        assertNotNull(response);
        assertEquals("Test Product", response.name());
        verify(productRepository).findById(id);
    }

    @Test
    void getProduct_NotFound_ThrowsException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProduct(id));
    }
}
