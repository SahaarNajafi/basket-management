package com.example.basketservice.basket;

import com.example.basketservice.basket.entity.Basket;
import com.example.basketservice.basket.repository.BasketRepository;
import com.example.basketservice.product.entity.Product;
import com.example.basketservice.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the {@code @Version} optimistic lock on {@link Basket} actually guards
 * against lost updates: two clients load the same basket, both edit it, and the
 * second write to commit is rejected with a conflict rather than silently
 * overwriting the first.
 */
@SpringBootTest
@ActiveProfiles("test")
class BasketConcurrencyIT {

    @Autowired
    private BasketRepository basketRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private PlatformTransactionManager txManager;

    private UUID basketId;

    @BeforeEach
    void setUp() {
        basketRepository.deleteAll();
        productRepository.deleteAll();

        Product product = productRepository.save(
                new Product("Widget", new BigDecimal("10.00"), 10));

        Basket basket = new Basket("customer-1");
        basket.addItem(product, 1);
        basketId = basketRepository.save(basket).getId();
    }

    @Test
    void concurrentEditsToTheSameBasketConflict() {
        // Two clients each load their own detached copy at the same version.
        Basket clientA = loadDetached();
        Basket clientB = loadDetached();

        // Client A finalises first and wins.
        clientA.checkout();
        basketRepository.saveAndFlush(clientA);

        // Client B was working from the now-stale version: its write is rejected.
        clientB.checkout();
        assertThatThrownBy(() -> basketRepository.saveAndFlush(clientB))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    /**
     * Loads a basket in its own transaction and initialises the item collection,
     * returning a detached copy — the equivalent of one client's read.
     */
    private Basket loadDetached() {
        return new TransactionTemplate(txManager).execute(status -> {
            Basket basket = basketRepository.findById(basketId).orElseThrow();
            assertThat(basket.getItems()).isNotEmpty(); // force lazy init before detach
            return basket;
        });
    }
}