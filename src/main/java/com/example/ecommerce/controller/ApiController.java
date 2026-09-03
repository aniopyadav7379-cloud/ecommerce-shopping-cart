package com.example.ecommerce.controller;

import com.example.ecommerce.config.CartIdProvider;
import com.example.ecommerce.dto.*;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.service.CartService;
import com.example.ecommerce.service.CheckoutService;
import com.example.ecommerce.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API layer. Controllers stay thin: they resolve the cart id, delegate
 * to services for every business rule, and translate the result into a DTO.
 * All error handling is centralized in GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final ProductService productService;
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final CartIdProvider cartIdProvider;

    public ApiController(ProductService productService, CartService cartService,
                          CheckoutService checkoutService, CartIdProvider cartIdProvider) {
        this.productService = productService;
        this.cartService = cartService;
        this.checkoutService = checkoutService;
        this.cartIdProvider = cartIdProvider;
    }

    // ===================== PRODUCTS =====================

    @GetMapping("/products")
    public List<ProductResponse> listProducts(@RequestParam(name = "q", required = false) String q) {
        List<Product> products = (q == null || q.isBlank()) ? productService.findAll() : productService.search(q);
        return products.stream().map(ProductResponse::from).collect(Collectors.toList());
    }

    @GetMapping("/products/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return ProductResponse.from(productService.findById(id));
    }

    @GetMapping("/products/search")
    public List<ProductResponse> searchProducts(@RequestParam(name = "q", required = false) String q) {
        return productService.search(q).stream().map(ProductResponse::from).collect(Collectors.toList());
    }

    // ===================== CART =====================

    @GetMapping("/cart")
    public CartResponse getCart(HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        return toCartResponse(cartService.getItems(cartId));
    }

    @PostMapping("/cart/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest body,
                                                 HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        cartService.addItem(cartId, body.getProductId(), body.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(toCartResponse(cartService.getItems(cartId)));
    }

    @PatchMapping("/cart/items/{productId}")
    public CartResponse updateItem(@PathVariable Long productId, @Valid @RequestBody UpdateCartItemRequest body,
                                    HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        cartService.setQuantity(cartId, productId, body.getQuantity());
        return toCartResponse(cartService.getItems(cartId));
    }

    @DeleteMapping("/cart/items/{productId}")
    public CartResponse removeItem(@PathVariable Long productId, HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        cartService.removeItem(cartId, productId);
        return toCartResponse(cartService.getItems(cartId));
    }

    @DeleteMapping("/cart")
    public CartResponse clearCart(HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        cartService.clearCart(cartId);
        return toCartResponse(cartService.getItems(cartId));
    }

    // ===================== CHECKOUT =====================

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest body,
                                                    HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        Order order = checkoutService.checkout(cartId, body.getCustomerName(), body.getEmail(), body.getShippingAddress());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/orders/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return OrderResponse.from(checkoutService.findOrderById(id));
    }

    private CartResponse toCartResponse(List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream().map(CartItemResponse::from).collect(Collectors.toList());
        int itemCount = items.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal total = itemResponses.stream().map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(itemResponses, itemCount, total);
    }
}
