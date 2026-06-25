package com.example.basketservice.basket.entity;

import com.example.basketservice.common.entity.BaseEntity;
import com.example.basketservice.common.exception.InvalidQuantityException;
import com.example.basketservice.product.entity.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A single line in a basket: a product, a quantity, and a snapshotted unit price.
 *
 * <p>The unit price is captured when the item is first added. Later quantity
 * changes do not reprice the existing basket line.
 */
@Entity
@Table(
        name = "basket_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_basket_product",
                columnNames = {"basket_id", "product_id"}
        )
)
public class BasketItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "basket_id", nullable = false)
    private Basket basket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Version
    private Long version;

    protected BasketItem() {
        // Required by JPA.
    }

    BasketItem(Basket basket, Product product, int quantity) {
        this.basket = Objects.requireNonNull(basket, "Basket is required");
        this.product = Objects.requireNonNull(product, "Product is required");
        setQuantity(quantity);
        this.unitPrice = Objects.requireNonNull(product.getPrice(), "Product price is required");
    }

    void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
        this.quantity = quantity;
    }

    void increaseQuantity(int delta) {
        setQuantity(this.quantity + delta);
    }

    boolean hasProduct(UUID productId) {
        return product.getId().equals(productId);
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public UUID getId() {
        return id;
    }

    public Basket getBasket() {
        return basket;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BasketItem other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}