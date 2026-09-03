package com.example.ecommerce.config;

import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds a realistic product catalogue on first startup only. Guarded by a
 * count check so restarting the application never inserts duplicate data.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ProductRepository productRepository;

    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Product catalogue already contains {} product(s); skipping seed.", productRepository.count());
            return;
        }

        List<Product> seedProducts = List.of(
                new Product(
                        "Wireless Headphones",
                        "Over-ear Bluetooth headphones with active noise cancellation, 30-hour battery life, and plush memory-foam ear cushions for all-day comfort.",
                        new BigDecimal("4999.00"), 25,
                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600"),
                new Product(
                        "Smart Watch",
                        "Fitness-focused smartwatch with heart-rate and SpO2 tracking, GPS, a bright always-on AMOLED display, and 7-day battery life.",
                        new BigDecimal("7999.00"), 18,
                        "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600"),
                new Product(
                        "Mechanical Keyboard",
                        "Tenkeyless mechanical keyboard with hot-swappable switches, per-key RGB backlighting, and a durable aluminum top plate.",
                        new BigDecimal("3499.00"), 30,
                        "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=600"),
                new Product(
                        "Running Shoes",
                        "Lightweight running shoes with responsive foam cushioning, breathable engineered mesh upper, and a durable rubber outsole.",
                        new BigDecimal("2999.00"), 40,
                        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600"),
                new Product(
                        "Laptop Backpack",
                        "Water-resistant 15.6-inch laptop backpack with a padded compartment, USB charging port, and multiple organizer pockets.",
                        new BigDecimal("1899.00"), 50,
                        "https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=600"),
                new Product(
                        "Smartphone",
                        "6.5-inch AMOLED smartphone with a 108MP triple camera system, 5G connectivity, and fast 67W charging.",
                        new BigDecimal("24999.00"), 12,
                        "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600"),
                new Product(
                        "Bluetooth Speaker",
                        "Portable waterproof Bluetooth speaker with 360-degree sound, 20-hour battery, and rugged drop-proof housing.",
                        new BigDecimal("2499.00"), 35,
                        "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=600"),
                new Product(
                        "4K Action Camera",
                        "Rugged 4K60 action camera with hydrophobic lens coating, image stabilization, and a full accessory mounting kit.",
                        new BigDecimal("8999.00"), 15,
                        "https://images.unsplash.com/photo-1526178613658-3f1622045557?w=600")
        );

        productRepository.saveAll(seedProducts);
        log.info("Seeded {} sample product(s).", seedProducts.size());
    }
}
