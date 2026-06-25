package com.example.basketservice.product.entity;

import com.example.basketservice.common.exception.InsufficientStockException;
import com.example.basketservice.common.exception.InvalidStockLevelException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A product that a customer can browse and add to a basket.
 *
 * <p>In production these records are owned by a separate catalogue/inventory
 * service. Here they are seeded locally (see {@code DataSeeder}) so the basket
 * service can be exercised end-to-end.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    /**
     * Unit price. Money is always modelled with BigDecimal, never double.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    /**
     * Quantity currently available in the warehouse.
     */
    @Column(nullable = false)
    private int availableQuantity;

    /**
     * Optimistic-lock guard against concurrent stock updates.
     */
    @Version
    private Long version;

    protected Product() {
        // Required by JPA.
    }

    public Product(String name, BigDecimal price, int availableQuantity) {
        this.name = Objects.requireNonNull(name, "Product name is required");
        this.price = Objects.requireNonNull(price, "Product price is required");
        validateStock(availableQuantity);
        this.availableQuantity = availableQuantity;
    }

    public void ensureStockAvailable(int requestedQuantity) {
        if (requestedQuantity > availableQuantity) {
            throw new InsufficientStockException(id, name, requestedQuantity, availableQuantity);
        }
    }


    private void validateStock(int stock) {
        if (stock < 0) {
            throw new InvalidStockLevelException(stock);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
