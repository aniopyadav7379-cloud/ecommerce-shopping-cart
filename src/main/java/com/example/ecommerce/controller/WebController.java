package com.example.ecommerce.controller;

import com.example.ecommerce.config.CartIdProvider;
import com.example.ecommerce.dto.CheckoutRequest;
import com.example.ecommerce.exception.InvalidQuantityException;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.service.CartService;
import com.example.ecommerce.service.CheckoutService;
import com.example.ecommerce.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Renders the Thymeleaf UI. Contains no business logic — everything here
 * delegates to the service layer and only shapes data for the view.
 */
@Controller
public class WebController {

    private final ProductService productService;
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final CartIdProvider cartIdProvider;

    public WebController(ProductService productService, CartService cartService,
                          CheckoutService checkoutService, CartIdProvider cartIdProvider) {
        this.productService = productService;
        this.cartService = cartService;
        this.checkoutService = checkoutService;
        this.cartIdProvider = cartIdProvider;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "q", required = false) String q, Model model,
                        HttpServletRequest request, HttpServletResponse response) {
        List<Product> products = (q == null || q.isBlank()) ? productService.findAll() : productService.search(q);
        model.addAttribute("products", products);
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("cartItemCount", cartItemCount(request, response));
        return "index";
    }

    @GetMapping("/product/{id}")
    public String productDetails(@PathVariable Long id, Model model,
                                  HttpServletRequest request, HttpServletResponse response) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("cartItemCount", cartItemCount(request, response));
        return "product";
    }

    @PostMapping("/product/{id}/add-to-cart")
    public String addToCart(@PathVariable Long id, @RequestParam(defaultValue = "1") Integer quantity,
                             HttpServletRequest request, HttpServletResponse response) {
        if (quantity == null || quantity < 1) {
            throw new InvalidQuantityException("Quantity must be at least 1.");
        }
        String cartId = cartIdProvider.resolveCartId(request, response);
        cartService.addItem(cartId, id, quantity);
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String viewCart(Model model, HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        List<CartItem> items = cartService.getItems(cartId);
        model.addAttribute("items", items);
        model.addAttribute("total", cartService.calculateTotal(cartId));
        model.addAttribute("cartItemCount", items.stream().mapToInt(CartItem::getQuantity).sum());
        return "cart";
    }

    @PostMapping("/cart/update")
    public String updateCartItem(@RequestParam Long productId, @RequestParam Integer quantity,
                                  HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        cartService.setQuantity(cartId, productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeCartItem(@RequestParam Long productId, HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        cartService.removeItem(cartId, productId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        cartService.clearCart(cartId);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkoutPage(Model model, HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        List<CartItem> items = cartService.getItems(cartId);
        model.addAttribute("items", items);
        model.addAttribute("total", cartService.calculateTotal(cartId));
        model.addAttribute("cartItemCount", items.stream().mapToInt(CartItem::getQuantity).sum());
        if (!model.containsAttribute("checkoutRequest")) {
            model.addAttribute("checkoutRequest", new CheckoutRequest());
        }
        return "checkout";
    }

    @PostMapping("/checkout")
    public String submitCheckout(@Valid @ModelAttribute("checkoutRequest") CheckoutRequest checkoutRequest,
                                  BindingResult bindingResult, Model model,
                                  HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);

        if (bindingResult.hasErrors()) {
            List<CartItem> items = cartService.getItems(cartId);
            model.addAttribute("items", items);
            model.addAttribute("total", cartService.calculateTotal(cartId));
            model.addAttribute("cartItemCount", items.stream().mapToInt(CartItem::getQuantity).sum());
            return "checkout";
        }

        Order order = checkoutService.checkout(cartId, checkoutRequest.getCustomerName(),
                checkoutRequest.getEmail(), checkoutRequest.getShippingAddress());
        return "redirect:/order/" + order.getId();
    }

    @GetMapping("/order/{id}")
    public String orderConfirmation(@PathVariable Long id, Model model,
                                     HttpServletRequest request, HttpServletResponse response) {
        Order order = checkoutService.findOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("cartItemCount", cartItemCount(request, response));
        return "order";
    }

    private int cartItemCount(HttpServletRequest request, HttpServletResponse response) {
        String cartId = cartIdProvider.resolveCartId(request, response);
        return cartService.getItems(cartId).stream().mapToInt(CartItem::getQuantity).sum();
    }
}
