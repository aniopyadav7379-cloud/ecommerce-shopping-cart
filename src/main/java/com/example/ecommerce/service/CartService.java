package com.example.ecommerce.service;

import com.example.ecommerce.exception.CartItemNotFoundException;
import com.example.ecommerce.exception.InsufficientStockException;
import com.example.ecommerce.exception.InvalidQuantityException;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Owns all shopping-cart business rules: adding, updating, removing items,
 * and computing totals. Cart items are persisted in the database keyed by a
 * cart id (the caller supplies the guest session's cart id), so the cart
 * survives page navigation and is never trusted from the frontend.
 */
@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    public CartService(CartItemRepository cartItemRepository, ProductService productService) {
        this.cartItemRepository = cartItemRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public List<CartItem> getItems(String cartId) {
        return cartItemRepository.findByCartIdOrderByIdAsc(cartId);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotal(String cartId) {
        return getItems(cartId).stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Adds a product to the cart. If the product is already present, the
     * requested quantity is added to the existing quantity and revalidated
     * against current stock as a whole.
     */
    @Transactional
    public CartItem addItem(String cartId, Long productId, int quantity) {
        requirePositiveQuantity(quantity);
        Product product = productService.findById(productId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseGet(() -> new CartItem(cartId, product, 0));

        int newQuantity = item.getQuantity() + quantity;
        validateStock(product, newQuantity);

        item.setQuantity(newQuantity);
        item.setProduct(product);
        return cartItemRepository.save(item);
    }

    /**
     * Sets the quantity of an existing cart item to an absolute value
     * (used by the PATCH endpoint and the cart page's quantity controls).
     */
    @Transactional
    public CartItem setQuantity(String cartId, Long productId, int quantity) {
        requirePositiveQuantity(quantity);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new CartItemNotFoundException(productId));

        validateStock(item.getProduct(), quantity);
        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    @Transactional
    public void removeItem(String cartId, Long productId) {
        cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new CartItemNotFoundException(productId));
        cartItemRepository.deleteByCartIdAndProductId(cartId, productId);
    }

    @Transactional
    public void clearCart(String cartId) {
        cartItemRepository.deleteByCartId(cartId);
    }

    private void validateStock(Product product, int requestedQuantity) {
        if (requestedQuantity > product.getStock()) {
            throw new InsufficientStockException(product.getName(), product.getStock());
        }
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity < 1) {
            throw new InvalidQuantityException("Quantity must be at least 1.");
        }
    }
}
