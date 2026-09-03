package com.example.ecommerce.service;

import com.example.ecommerce.exception.EmptyCartException;
import com.example.ecommerce.exception.InsufficientStockException;
import com.example.ecommerce.exception.OrderNotFoundException;
import com.example.ecommerce.exception.ProductNotFoundException;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.OrderItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orchestrates the checkout flow. The entire operation runs in a single
 * database transaction: cart validation, stock revalidation, order and
 * order-item creation, stock deduction, and clearing the cart either all
 * succeed together or all roll back together. Nothing here trusts totals
 * or prices supplied by the frontend — everything is recomputed from the
 * database inside the transaction.
 */
@Service
public class CheckoutService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public CheckoutService(CartItemRepository cartItemRepository,
                            ProductRepository productRepository,
                            OrderRepository orderRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order checkout(String cartId, String customerName, String email, String shippingAddress) {
        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByIdAsc(cartId);
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }

        Order order = new Order();
        order.setCustomerName(customerName.trim());
        order.setEmail(email.trim());
        order.setShippingAddress(shippingAddress.trim());

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            // Reload the product under a pessimistic write lock so concurrent
            // checkouts can't both oversell the same remaining stock.
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ProductNotFoundException(cartItem.getProduct().getId()));

            int requestedQuantity = cartItem.getQuantity();
            if (requestedQuantity > product.getStock()) {
                throw new InsufficientStockException(product.getName(), product.getStock());
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(requestedQuantity));
            total = total.add(subtotal);

            OrderItem orderItem = new OrderItem(product, product.getName(), product.getPrice(), requestedQuantity, subtotal);
            order.addItem(orderItem);

            product.setStock(product.getStock() - requestedQuantity);
            productRepository.save(product);
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteByCartId(cartId);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Order findOrderById(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
