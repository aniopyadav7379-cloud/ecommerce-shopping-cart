package com.example.ecommerce.service;

import com.example.ecommerce.exception.CartItemNotFoundException;
import com.example.ecommerce.exception.InsufficientStockException;
import com.example.ecommerce.exception.InvalidQuantityException;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final String CART_ID = "cart-123";

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductService productService;

    private CartService cartService;

    private Product product;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository, productService);
        product = new Product("Smart Watch", "Fitness tracker", new BigDecimal("7999.00"), 5, "img.jpg");
        product.setId(1L);
    }

    @Test
    void addItem_createsNewCartItem_whenNotAlreadyInCart() {
        when(productService.findById(1L)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem result = cartService.addItem(CART_ID, 1L, 3);

        assertThat(result.getQuantity()).isEqualTo(3);
        assertThat(result.getCartId()).isEqualTo(CART_ID);
    }

    @Test
    void addItem_addsSameProductTwice_accumulatesQuantity() {
        CartItem existing = new CartItem(CART_ID, product, 3);
        when(productService.findById(1L)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, 1L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // Product has stock 5; already 3 in cart, adding 2 more should reach exactly 5 (allowed)
        CartItem result = cartService.addItem(CART_ID, 1L, 2);

        assertThat(result.getQuantity()).isEqualTo(5);
    }

    @Test
    void addItem_rejectsWhenCombinedQuantityExceedsStock() {
        CartItem existing = new CartItem(CART_ID, product, 3);
        when(productService.findById(1L)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, 1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cartService.addItem(CART_ID, 1L, 3))
                .isInstanceOf(InsufficientStockException.class);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> cartService.addItem(CART_ID, 1L, 0))
                .isInstanceOf(InvalidQuantityException.class);
        verifyNoInteractions(productService);
    }

    @Test
    void setQuantity_updatesExistingItem() {
        CartItem existing = new CartItem(CART_ID, product, 2);
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, 1L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem result = cartService.setQuantity(CART_ID, 1L, 4);

        assertThat(result.getQuantity()).isEqualTo(4);
    }

    @Test
    void setQuantity_rejectsWhenAboveStock() {
        CartItem existing = new CartItem(CART_ID, product, 2);
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, 1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cartService.setQuantity(CART_ID, 1L, 100))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void removeItem_deletesExistingItem() {
        CartItem existing = new CartItem(CART_ID, product, 2);
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, 1L)).thenReturn(Optional.of(existing));

        cartService.removeItem(CART_ID, 1L);

        verify(cartItemRepository).deleteByCartIdAndProductId(CART_ID, 1L);
    }

    @Test
    void removeItem_throwsWhenItemMissing() {
        when(cartItemRepository.findByCartIdAndProductId(CART_ID, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(CART_ID, 1L))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void clearCart_deletesAllItemsForCart() {
        cartService.clearCart(CART_ID);

        verify(cartItemRepository).deleteByCartId(CART_ID);
    }

    @Test
    void calculateTotal_sumsAllItemSubtotals() {
        Product second = new Product("Keyboard", "Mechanical", new BigDecimal("3000.00"), 10, "img2.jpg");
        second.setId(2L);
        CartItem item1 = new CartItem(CART_ID, product, 2);   // 7999 * 2 = 15998
        CartItem item2 = new CartItem(CART_ID, second, 1);    // 3000 * 1 = 3000
        when(cartItemRepository.findByCartIdOrderByIdAsc(CART_ID)).thenReturn(List.of(item1, item2));

        BigDecimal total = cartService.calculateTotal(CART_ID);

        assertThat(total).isEqualByComparingTo(new BigDecimal("18998.00"));
    }
}
