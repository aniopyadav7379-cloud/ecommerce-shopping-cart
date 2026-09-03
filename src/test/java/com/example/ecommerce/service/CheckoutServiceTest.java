package com.example.ecommerce.service;

import com.example.ecommerce.exception.EmptyCartException;
import com.example.ecommerce.exception.InsufficientStockException;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class CheckoutServiceTest {

    private static final String CART_ID = "cart-abc";

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    private CheckoutService checkoutService;

    private Product product;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(cartItemRepository, productRepository, orderRepository);
        product = new Product("Running Shoes", "Lightweight", new BigDecimal("2999.00"), 5, "img.jpg");
        product.setId(1L);
    }

    @Test
    void checkout_throwsEmptyCartException_whenCartIsEmpty() {
        when(cartItemRepository.findByCartIdOrderByIdAsc(CART_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> checkoutService.checkout(CART_ID, "Anoop Yadav", "a@example.com", "New Delhi"))
                .isInstanceOf(EmptyCartException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_rejectsInsufficientStock_andDoesNotCreateOrder() {
        CartItem cartItem = new CartItem(CART_ID, product, 10); // more than the 5 in stock
        when(cartItemRepository.findByCartIdOrderByIdAsc(CART_ID)).thenReturn(List.of(cartItem));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> checkoutService.checkout(CART_ID, "Anoop Yadav", "a@example.com", "New Delhi"))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByCartId(any());
    }

    @Test
    void checkout_createsOrder_deductsStock_andClearsCart_onSuccess() {
        CartItem cartItem = new CartItem(CART_ID, product, 2);
        when(cartItemRepository.findByCartIdOrderByIdAsc(CART_ID)).thenReturn(List.of(cartItem));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1001L);
            return o;
        });

        Order result = checkoutService.checkout(CART_ID, "Anoop Yadav", "anoop@example.com", "New Delhi, India");

        assertThat(result.getId()).isEqualTo(1001L);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("5998.00"));
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("Running Shoes");
        assertThat(result.getItems().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("2999.00"));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getStock()).isEqualTo(3); // 5 - 2

        verify(cartItemRepository).deleteByCartId(CART_ID);
    }

    @Test
    void checkout_neverAllowsNegativeStock() {
        Product limited = new Product("Rare Item", "Only one left", new BigDecimal("100.00"), 1, "img.jpg");
        limited.setId(2L);
        CartItem cartItem = new CartItem(CART_ID, limited, 2); // requesting more than available
        when(cartItemRepository.findByCartIdOrderByIdAsc(CART_ID)).thenReturn(List.of(cartItem));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(limited));

        assertThatThrownBy(() -> checkoutService.checkout(CART_ID, "Test User", "t@example.com", "Address"))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(limited.getStock()).isEqualTo(1); // untouched
    }
}
