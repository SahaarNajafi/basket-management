package com.example.basketservice.basket.entity;

import com.example.basketservice.common.entity.BaseEntity;
import com.example.basketservice.common.exception.BasketItemNotFoundException;
import com.example.basketservice.common.exception.BasketNotModifiableException;
import com.example.basketservice.common.exception.EmptyBasketException;
import com.example.basketservice.common.exception.InvalidQuantityException;
import com.example.basketservice.product.entity.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Aggregate root for a customer's basket.
 *
 * <p>All basket state changes go through behaviour methods so invariants such as
 * unique product lines and immutability after checkout stay inside the domain model.
 */
@Entity
@Table(name = "baskets")
public class Basket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the owning customer. Optional in this assignment because auth
     * is out of scope. In production this would usually be non-null and indexed.
     */
    @Column(name = "customer_id")
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BasketStatus status = BasketStatus.ACTIVE;

    @OneToMany(
            mappedBy = "basket",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<BasketItem> items = new ArrayList<>();

    @Version
    private Long version;

    protected Basket() {
        // Required by JPA.
    }

    public Basket(String customerId) {
        this.customerId = customerId;
    }

    public void addItem(Product product, int quantity) {
        ensureModifiable();
        validateQuantity(quantity);

        // Stock must cover the resulting line quantity, not just the delta,
        // otherwise repeated adds can oversell within a single basket.
        int resultingQuantity = getCurrentQuantity(product.getId()) + quantity;
        product.ensureStockAvailable(resultingQuantity);

        findItem(product.getId())
                .ifPresentOrElse(
                        item -> item.increaseQuantity(quantity),
                        () -> items.add(new BasketItem(this, product, quantity))
                );
    }

    public void setItemQuantity(Product product, int quantity) {
        ensureModifiable();
        validateQuantity(quantity);

        // The new quantity is absolute, so validate it directly against stock.
        product.ensureStockAvailable(quantity);

        findItem(product.getId())
                .ifPresentOrElse(
                        item -> item.setQuantity(quantity),
                        () -> items.add(new BasketItem(this, product, quantity))
                );
    }

    /**
     * Removes the line for the given product.
     */
    public void removeItem(UUID productId) {
        ensureModifiable();

        boolean removed = items.removeIf(i -> i.hasProduct(productId));

        if (!removed) {
            throw new BasketItemNotFoundException(id, productId);
        }
    }

    public void checkout() {
        ensureModifiable();

        if (items.isEmpty()) {
            throw new EmptyBasketException(id);
        }

        status = BasketStatus.CHECKED_OUT;
    }


    private void ensureModifiable() {
        if (status != BasketStatus.ACTIVE) {
            throw new BasketNotModifiableException(id, status);
        }
    }


    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
    }

    public int getCurrentQuantity(UUID productId) {
        return findItem(productId)
                .map(BasketItem::getQuantity)
                .orElse(0);
    }

    private Optional<BasketItem> findItem(UUID productId) {
        return items.stream()
                .filter(i -> i.hasProduct(productId))
                .findFirst();
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(BasketItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BasketStatus getStatus() {
        return status;
    }

    public List<BasketItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Basket other)) {
            return false;
        }

        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}