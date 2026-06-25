package com.example.basketservice.common.config;

import com.example.basketservice.product.entity.Product;
import com.example.basketservice.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pre-populates the catalogue with imaginary products on startup.
 *
 * <p>This stands in for the external catalogue/inventory service, which is out
 * of scope. It is disabled under the "test" profile so tests control their own
 * fixtures. In production this class would not exist.
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final ProductRepository productRepository;

    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional // Ensure the seed operation is atomic
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Database already populated. Skipping seeding.");
            return;
        }

        log.info("Seeding initial product catalogue...");

        List<Product> products = List.of(
                new Product("Mechanical Keyboard", new BigDecimal("89.99"), 50),
                new Product("Wireless Mouse", new BigDecimal("39.50"), 120),
                new Product("27\" 4K Monitor", new BigDecimal("329.00"), 25),
                new Product("USB-C Hub", new BigDecimal("49.95"), 80),
                new Product("Noise-Cancelling Headphones", new BigDecimal("199.99"), 40),
                new Product("Laptop Stand", new BigDecimal("34.00"), 100),
                new Product("Webcam 1080p", new BigDecimal("59.99"), 0),
                new Product("Desk Mat", new BigDecimal("19.99"), 200)
        );

        productRepository.saveAll(products);
        log.info("Successfully seeded {} products.", products.size());
    }
}

