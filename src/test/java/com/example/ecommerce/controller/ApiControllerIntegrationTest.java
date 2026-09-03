package com.example.ecommerce.controller;

import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests against the real REST API, an actual (in-memory) H2
 * database, and the full Spring context. Each test's HTTP session carries
 * the CART_ID cookie set by the first response, exactly like a browser
 * would, so cart state naturally persists across requests within a test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long inStockProductId;
    private Long lowStockProductId;

    @BeforeEach
    void setUp() {
        // Clear child rows first so deleting products never violates a
        // foreign-key constraint left over from a previous test method.
        cartItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        Product headphones = productRepository.save(
                new Product("Wireless Headphones", "Noise cancelling headphones", new BigDecimal("4999.00"), 10, "img1.jpg"));
        Product rareItem = productRepository.save(
                new Product("Rare Collectible", "Only two left", new BigDecimal("199.00"), 2, "img2.jpg"));
        inStockProductId = headphones.getId();
        lowStockProductId = rareItem.getId();
    }

    // ===================== PRODUCT LISTING / SEARCH / NOT FOUND =====================

    @Test
    void listProducts_returnsAllSeededProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void searchProducts_isCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "WIRELESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Wireless Headphones")));
    }

    @Test
    void getProduct_returns404_whenProductDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("PRODUCT_NOT_FOUND")));
    }

    // ===================== CART =====================

    @Test
    void addToCart_thenAddSameProductAgain_accumulatesQuantity() throws Exception {
        MvcResult first = addItem(inStockProductId, 2, null);
        String cookie = extractCartCookie(first);

        mockMvc.perform(post("/api/cart/items")
                        .cookie(cookie(cookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + inStockProductId + ",\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity", is(5)))
                .andExpect(jsonPath("$.itemCount", is(5)));
    }

    @Test
    void addToCart_rejectsQuantityAboveStock() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + lowStockProductId + ",\"quantity\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("INSUFFICIENT_STOCK")));
    }

    @Test
    void updateCartItem_changesQuantity() throws Exception {
        MvcResult added = addItem(inStockProductId, 2, null);
        String cookie = extractCartCookie(added);

        mockMvc.perform(patch("/api/cart/items/{id}", inStockProductId)
                        .cookie(cookie(cookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity", is(4)));
    }

    @Test
    void removeCartItem_deletesIt() throws Exception {
        MvcResult added = addItem(inStockProductId, 2, null);
        String cookie = extractCartCookie(added);

        mockMvc.perform(delete("/api/cart/items/{id}", inStockProductId).cookie(cookie(cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void clearCart_removesAllItems() throws Exception {
        MvcResult added = addItem(inStockProductId, 2, null);
        String cookie = extractCartCookie(added);
        addItem(lowStockProductId, 1, cookie);

        mockMvc.perform(delete("/api/cart").cookie(cookie(cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total", is(0)));
    }

    @Test
    void cartTotal_isCalculatedServerSide() throws Exception {
        MvcResult added = addItem(inStockProductId, 2, null); // 4999.00 * 2 = 9998.00
        mockMvc.perform(get("/api/cart").cookie(cookie(extractCartCookie(added))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(9998.00)));
    }

    // ===================== CHECKOUT =====================

    @Test
    void checkout_rejectsEmptyCart() throws Exception {
        mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("EMPTY_CART")));
    }

    @Test
    void checkout_rejectsInvalidCustomerInformation() throws Exception {
        MvcResult added = addItem(inStockProductId, 1, null);
        String cookie = extractCartCookie(added);

        String invalidJson = "{\"customerName\":\"\",\"email\":\"not-an-email\",\"shippingAddress\":\"x\"}";

        mockMvc.perform(post("/api/checkout")
                        .cookie(cookie(cookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void checkout_rejectsInsufficientStock() throws Exception {
        MvcResult added = addItem(lowStockProductId, 2, null); // exactly at stock limit, allowed in cart
        String cookie = extractCartCookie(added);

        // Reduce stock below what's in the cart to simulate a race with another shopper
        Product rare = productRepository.findById(lowStockProductId).orElseThrow();
        rare.setStock(1);
        productRepository.save(rare);

        mockMvc.perform(post("/api/checkout")
                        .cookie(cookie(cookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("INSUFFICIENT_STOCK")));
    }

    @Test
    void checkout_succeeds_deductsStock_andClearsCart() throws Exception {
        MvcResult added = addItem(inStockProductId, 3, null); // 4999.00 * 3 = 14997.00
        String cookie = extractCartCookie(added);

        mockMvc.perform(post("/api/checkout")
                        .cookie(cookie(cookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.totalAmount", is(14997.00)))
                .andExpect(jsonPath("$.items[0].productName", is("Wireless Headphones")));

        // stock deducted
        Product refreshed = productRepository.findById(inStockProductId).orElseThrow();
        assertThat(refreshed.getStock()).isEqualTo(7); // 10 - 3

        // cart cleared
        mockMvc.perform(get("/api/cart").cookie(cookie(cookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    // ===================== helpers =====================

    private MvcResult addItem(Long productId, int quantity, String existingCartCookie) throws Exception {
        var request = post("/api/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}");
        if (existingCartCookie != null) {
            request.cookie(cookie(existingCartCookie));
        }
        return mockMvc.perform(request).andExpect(status().isCreated()).andReturn();
    }

    private String extractCartCookie(MvcResult result) {
        jakarta.servlet.http.Cookie c = result.getResponse().getCookie("CART_ID");
        assertThat(c).isNotNull();
        return c.getValue();
    }

    private jakarta.servlet.http.Cookie cookie(String value) {
        return new jakarta.servlet.http.Cookie("CART_ID", value);
    }

    private String validCheckoutJson() {
        return "{\"customerName\":\"Anoop Yadav\",\"email\":\"anoop@example.com\",\"shippingAddress\":\"New Delhi, India\"}";
    }
}
