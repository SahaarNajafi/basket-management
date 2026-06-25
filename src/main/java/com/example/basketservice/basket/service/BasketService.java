package com.example.basketservice.basket.service;

import com.example.basketservice.basket.dto.AddItemRequest;
import com.example.basketservice.basket.dto.BasketResponse;
import com.example.basketservice.basket.dto.CheckoutResponse;
import com.example.basketservice.basket.dto.CreateBasketRequest;
import com.example.basketservice.basket.entity.Basket;
import com.example.basketservice.basket.entity.BasketStatus;
import com.example.basketservice.basket.mapper.BasketMapper;
import com.example.basketservice.basket.repository.BasketRepository;
import com.example.basketservice.common.exception.ResourceNotFoundException;
import com.example.basketservice.product.entity.Product;
import com.example.basketservice.product.repository.ProductRepository;
import com.example.basketservice.basket.validation.StockValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service coordinating basket use cases.
 * <p>
 * Each public method represents one use case. Stock is validated when items are
 * added/updated and revalidated at checkout, but warehouse stock is not decremented
 * here. In a production system, stock reservation/decrementing would usually be
 * coordinated with an inventory or order service.
 */
@Service
@Transactional(readOnly = true)
public class BasketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasketService.class);

    private final BasketRepository basketRepository;
    private final ProductRepository productRepository;
    private final StockValidator stockValidator;
    private final BasketMapper mapper;

    public BasketService(BasketRepository basketRepository,
                         ProductRepository productRepository, StockValidator stockValidator,
                         BasketMapper mapper) {
        this.basketRepository = basketRepository;
        this.productRepository = productRepository;
        this.stockValidator = stockValidator;
        this.mapper = mapper;
    }

    @Transactional
    public BasketResponse createBasket(CreateBasketRequest request) {
        String customerId = request != null ? request.customerId() : null;

        Basket basket = basketRepository.save(new Basket(customerId));

        LOGGER.info("Created basket {} for customer {}", basket.getId(), customerId);
        return mapper.toBasketResponse(basket);
    }

    public BasketResponse getBasket(UUID basketId) {
        Basket basket = findBasket(basketId);
        return mapper.toBasketResponse(basket);
    }

    /**
     * Adds the requested quantity on top of any existing quantity for the product.
     */
    @Transactional
    public BasketResponse addItem(UUID basketId, AddItemRequest request) {

        Basket basket = findBasket(basketId);
        Product product = getProduct(request.productId());

        basket.addItem(product, request.quantity());

        return mapper.toBasketResponse(basket);
    }

    /**
     * Sets the absolute quantity for an existing product in the basket.
     */
    @Transactional
    public BasketResponse updateItem(UUID basketId, UUID productId, int quantity) {

        Basket basket = findBasket(basketId);
        Product product = getProduct(productId);

        basket.setItemQuantity(product, quantity);

        return mapper.toBasketResponse(basket);
    }

    @Transactional
    public BasketResponse removeItem(UUID basketId, UUID productId) {

        Basket basket = findBasket(basketId);
        basket.removeItem(productId);

        return mapper.toBasketResponse(basket);
    }

    /**
     * Finalises the basket and returns the checkout summary.
     * <p>
     * This method is idempotent: if the basket is already checked out, the existing
     * checkout summary is returned.
     */
    @Transactional
    public CheckoutResponse checkout(UUID basketId) {

        Basket basket = findBasket(basketId);

        // Idempotent: re-checking-out an already finalised basket returns the
        // same summary rather than failing.
        if (basket.getStatus() == BasketStatus.CHECKED_OUT) {
            return mapper.toCheckoutResponse(basket);
        }

        stockValidator.validate(basket.getItems());

        basket.checkout();

        return mapper.toCheckoutResponse(basket);
    }

    private Basket findBasket(UUID basketId) {
        return basketRepository.findWithItemsById(basketId)
                .orElseThrow(() -> new ResourceNotFoundException("Basket", basketId));
    }

    private Product getProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }


}