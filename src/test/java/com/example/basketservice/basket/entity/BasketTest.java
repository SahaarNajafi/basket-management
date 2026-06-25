package com.example.basketservice.basket.entity;

import com.example.basketservice.common.exception.BasketItemNotFoundException;
import com.example.basketservice.common.exception.BasketNotModifiableException;
import com.example.basketservice.common.exception.EmptyBasketException;
import com.example.basketservice.common.exception.InsufficientStockException;
import com.example.basketservice.common.exception.InvalidQuantityException;
import com.example.basketservice.product.entity.Product;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the {@link Basket} aggregate. These exercise the domain rules
 * directly (no Spring, no database, no mocks of the entity itself), because the
 * interesting behaviour — line merging, the price snapshot, stock guards and the
 * post-checkout freeze — lives in the model, not in the service.
 */
class BasketTest {

    @Nested
    class AddingItems {

        @Test
        void addsANewLineWithLineTotal() {
            Basket basket = new Basket("customer-1");
            Product keyboard = product("Keyboard", "89.99", 10);

            basket.addItem(keyboard, 2);

            assertThat(basket.getItems()).hasSize(1);
            assertThat(basket.getCurrentQuantity(keyboard.getId())).isEqualTo(2);
            assertThat(basket.getTotalAmount()).isEqualByComparingTo("179.98");
        }

        @Test
        void mergesRepeatedAddsIntoASingleLine() {
            Basket basket = new Basket("customer-1");
            Product mouse = product("Mouse", "20.00", 10);

            basket.addItem(mouse, 2);
            basket.addItem(mouse, 3);

            assertThat(basket.getItems()).hasSize(1);
            assertThat(basket.getCurrentQuantity(mouse.getId())).isEqualTo(5);
            assertThat(basket.getTotalAmount()).isEqualByComparingTo("100.00");
        }

        @Test
        void sumsTotalAcrossMultipleLines() {
            Basket basket = new Basket("customer-1");
            basket.addItem(product("Keyboard", "89.99", 10), 1);
            basket.addItem(product("Mouse", "20.00", 10), 2);

            assertThat(basket.getItems()).hasSize(2);
            assertThat(basket.getTotalAmount()).isEqualByComparingTo("129.99");
        }

        @Test
        void rejectsNonPositiveQuantity() {
            Basket basket = new Basket("customer-1");
            Product keyboard = product("Keyboard", "89.99", 10);

            assertThatThrownBy(() -> basket.addItem(keyboard, 0))
                    .isInstanceOf(InvalidQuantityException.class);
        }
    }

    @Nested
    class PriceSnapshot {

        @Test
        void lineTotalIsUnaffectedByLaterCataloguePriceChanges() {
            Basket basket = new Basket("customer-1");
            Product keyboard = product("Keyboard", "100.00", 10);

            basket.addItem(keyboard, 2);

            // The catalogue later reprices the product...
            setField(keyboard, "price", new BigDecimal("999.00"));

            // ...but the basket line keeps the price captured at add-time.
            assertThat(basket.getTotalAmount()).isEqualByComparingTo("200.00");
        }
    }

    @Nested
    class StockGuards {

        @Test
        void rejectsAddWhenSingleQuantityExceedsStock() {
            Basket basket = new Basket("customer-1");
            Product scarce = product("Scarce", "10.00", 3);

            assertThatThrownBy(() -> basket.addItem(scarce, 4))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        void rejectsIncrementWhenRunningTotalExceedsStock() {
            Basket basket = new Basket("customer-1");
            Product limited = product("Limited", "10.00", 10);

            basket.addItem(limited, 6); // ok: 6 <= 10

            // 6 + 6 = 12 > 10 — must be rejected on the resulting total,
            // not just on the 6-unit delta.
            assertThatThrownBy(() -> basket.addItem(limited, 6))
                    .isInstanceOf(InsufficientStockException.class);

            assertThat(basket.getCurrentQuantity(limited.getId())).isEqualTo(6);
        }

        @Test
        void rejectsAbsoluteUpdateThatExceedsStock() {
            Basket basket = new Basket("customer-1");
            Product limited = product("Limited", "10.00", 10);

            basket.addItem(limited, 1);

            assertThatThrownBy(() -> basket.setItemQuantity(limited, 11))
                    .isInstanceOf(InsufficientStockException.class);
        }
    }

    @Nested
    class UpdatingAndRemoving {

        @Test
        void setItemQuantitySetsTheAbsoluteValue() {
            Basket basket = new Basket("customer-1");
            Product mouse = product("Mouse", "20.00", 10);

            basket.addItem(mouse, 2);
            basket.setItemQuantity(mouse, 5);

            assertThat(basket.getCurrentQuantity(mouse.getId())).isEqualTo(5);
        }

        @Test
        void setItemQuantityCreatesTheLineWhenAbsent() {
            Basket basket = new Basket("customer-1");
            Product mouse = product("Mouse", "20.00", 10);

            basket.setItemQuantity(mouse, 3);

            assertThat(basket.getItems()).hasSize(1);
            assertThat(basket.getCurrentQuantity(mouse.getId())).isEqualTo(3);
        }

        @Test
        void removeDropsTheLine() {
            Basket basket = new Basket("customer-1");
            Product mouse = product("Mouse", "20.00", 10);
            basket.addItem(mouse, 1);

            basket.removeItem(mouse.getId());

            assertThat(basket.getItems()).isEmpty();
            assertThat(basket.getTotalAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void removingAnAbsentLineFails() {
            Basket basket = new Basket("customer-1");

            assertThatThrownBy(() -> basket.removeItem(UUID.randomUUID()))
                    .isInstanceOf(BasketItemNotFoundException.class);
        }
    }

    @Nested
    class Checkout {

        @Test
        void freezesTheBasket() {
            Basket basket = new Basket("customer-1");
            basket.addItem(product("Keyboard", "89.99", 10), 1);

            basket.checkout();

            assertThat(basket.getStatus()).isEqualTo(BasketStatus.CHECKED_OUT);
        }

        @Test
        void rejectsCheckoutOfAnEmptyBasket() {
            Basket basket = new Basket("customer-1");

            assertThatThrownBy(basket::checkout)
                    .isInstanceOf(EmptyBasketException.class);
        }

        @Test
        void blocksModificationAfterCheckout() {
            Basket basket = new Basket("customer-1");
            Product keyboard = product("Keyboard", "89.99", 10);
            basket.addItem(keyboard, 1);
            basket.checkout();

            assertThatThrownBy(() -> basket.addItem(keyboard, 1))
                    .isInstanceOf(BasketNotModifiableException.class);
            assertThatThrownBy(() -> basket.setItemQuantity(keyboard, 2))
                    .isInstanceOf(BasketNotModifiableException.class);
            assertThatThrownBy(() -> basket.removeItem(keyboard.getId()))
                    .isInstanceOf(BasketNotModifiableException.class);
        }
    }

    // --- helpers -------------------------------------------------------------
    // Products are persistence-managed, so in a pure unit test we assign the id
    // (normally generated by JPA) ourselves to keep the domain logic exercisable.

    private static Product product(String name, String price, int stock) {
        Product product = new Product(name, new BigDecimal(price), stock);
        setField(product, "id", UUID.randomUUID());
        return product;
    }

    private static void setField(Object target, String field, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field " + field, e);
        }
    }
}