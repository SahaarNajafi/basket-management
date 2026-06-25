package com.example.basketservice.basket.service;

import com.example.basketservice.basket.dto.AddItemRequest;
import com.example.basketservice.basket.dto.BasketResponse;
import com.example.basketservice.basket.dto.CheckoutResponse;
import com.example.basketservice.basket.dto.CreateBasketRequest;
import com.example.basketservice.basket.entity.Basket;
import com.example.basketservice.basket.mapper.BasketMapper;
import com.example.basketservice.basket.repository.BasketRepository;
import com.example.basketservice.basket.validation.StockValidator;
import com.example.basketservice.common.exception.ResourceNotFoundException;
import com.example.basketservice.product.entity.Product;
import com.example.basketservice.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasketServiceTest {

    @Mock
    private BasketRepository basketRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BasketMapper basketMapper;

    @Mock
    private StockValidator stockValidator;

    @InjectMocks
    private BasketService basketService;

    private UUID basketId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        basketId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Nested
    class CreateBasketTests {

        @Test
        void shouldCreateBasket() {

            CreateBasketRequest request =
                    new CreateBasketRequest("customer-1");

            Basket basket = new Basket("customer-1");

            BasketResponse response =
                    mock(BasketResponse.class);

            when(basketRepository.save(any(Basket.class)))
                    .thenReturn(basket);

            when(basketMapper.toBasketResponse(basket))
                    .thenReturn(response);

            BasketResponse result =
                    basketService.createBasket(request);

            assertThat(result).isEqualTo(response);

            verify(basketRepository).save(any(Basket.class));
            verify(basketMapper).toBasketResponse(basket);
        }

        @Test
        void shouldCreateBasketWithoutCustomerId() {

            Basket basket = new Basket(null);

            BasketResponse response =
                    mock(BasketResponse.class);

            when(basketRepository.save(any(Basket.class)))
                    .thenReturn(basket);

            when(basketMapper.toBasketResponse(basket))
                    .thenReturn(response);

            BasketResponse result =
                    basketService.createBasket(null);

            assertThat(result).isEqualTo(response);

            verify(basketRepository).save(any(Basket.class));
        }
    }

    @Nested
    class GetBasketTests {

        @Test
        void shouldReturnBasket() {

            Basket basket = mock(Basket.class);
            BasketResponse response = mock(BasketResponse.class);

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.of(basket));

            when(basketMapper.toBasketResponse(basket))
                    .thenReturn(response);

            BasketResponse result =
                    basketService.getBasket(basketId);

            assertThat(result).isEqualTo(response);

            verify(basketRepository).findWithItemsById(basketId);
            verify(basketMapper).toBasketResponse(basket);
        }

        @Test
        void shouldThrowWhenBasketNotFound() {

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    basketService.getBasket(basketId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(basketRepository).findWithItemsById(basketId);
        }
    }

    @Nested
    class AddItemTests {

        @Test
        void shouldAddItemToBasket() {

            Basket basket = mock(Basket.class);
            Product product = mock(Product.class);

            BasketResponse response =
                    mock(BasketResponse.class);

            AddItemRequest request =
                    new AddItemRequest(productId, 2);

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.of(basket));

            when(productRepository.findById(productId))
                    .thenReturn(Optional.of(product));

            when(basketMapper.toBasketResponse(basket))
                    .thenReturn(response);

            BasketResponse result =
                    basketService.addItem(basketId, request);

            assertThat(result).isEqualTo(response);

            verify(basket).addItem(product, 2);
            verify(basketMapper).toBasketResponse(basket);
        }

        @Test
        void shouldThrowWhenBasketNotFound() {

            AddItemRequest request =
                    new AddItemRequest(productId, 2);

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    basketService.addItem(basketId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(productRepository, never()).findById(any());
        }

        @Test
        void shouldThrowWhenProductNotFound() {

            Basket basket = mock(Basket.class);

            AddItemRequest request =
                    new AddItemRequest(productId, 2);

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.of(basket));

            when(productRepository.findById(productId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    basketService.addItem(basketId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

        }
    }

    @Nested
    class UpdateItemTests {

        @Test
        void shouldUpdateItemQuantity() {

            Basket basket = mock(Basket.class);
            Product product = mock(Product.class);

            BasketResponse response =
                    mock(BasketResponse.class);

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.of(basket));

            when(productRepository.findById(productId))
                    .thenReturn(Optional.of(product));

            when(basketMapper.toBasketResponse(basket))
                    .thenReturn(response);

            BasketResponse result =
                    basketService.updateItem(
                            basketId,
                            productId,
                            5);

            assertThat(result).isEqualTo(response);

            verify(basket).setItemQuantity(product, 5);
            verify(basketMapper).toBasketResponse(basket);
        }

        @Test
        void shouldThrowWhenBasketNotFound() {

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    basketService.updateItem(
                            basketId,
                            productId,
                            5))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(productRepository, never()).findById(any());
        }

        @Test
        void shouldThrowWhenProductNotFound() {

            Basket basket = mock(Basket.class);

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.of(basket));

            when(productRepository.findById(productId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    basketService.updateItem(
                            basketId,
                            productId,
                            5))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class RemoveItemTests {

        @Test
        void shouldRemoveItem() {

            Basket basket = mock(Basket.class);

            BasketResponse response =
                    mock(BasketResponse.class);

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.of(basket));

            when(basketMapper.toBasketResponse(basket))
                    .thenReturn(response);

            BasketResponse result =
                    basketService.removeItem(
                            basketId,
                            productId);

            assertThat(result).isEqualTo(response);

            verify(basket).removeItem(productId);
            verify(basketMapper).toBasketResponse(basket);
        }

        @Test
        void shouldThrowWhenBasketNotFound() {

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    basketService.removeItem(
                            basketId,
                            productId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class CheckoutTests {

        @Test
        void shouldCheckoutBasket() {

            Basket basket = mock(Basket.class);

            CheckoutResponse response =
                    mock(CheckoutResponse.class);

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.of(basket));

            when(basketMapper.toCheckoutResponse(basket))
                    .thenReturn(response);

            CheckoutResponse result =
                    basketService.checkout(basketId);

            assertThat(result).isEqualTo(response);

            verify(basket).checkout();
            verify(basketMapper).toCheckoutResponse(basket);
        }

        @Test
        void shouldThrowWhenBasketNotFound() {

            when(basketRepository.findWithItemsById(basketId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    basketService.checkout(basketId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}