package com.example.ecommerce.service;

import com.example.ecommerce.exception.ProductNotFoundException;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product headphones;

    @BeforeEach
    void setUp() {
        headphones = new Product("Wireless Headphones", "Great sound", new BigDecimal("4999.00"), 10, "img.jpg");
        headphones.setId(1L);
    }

    @Test
    void findAll_returnsAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(headphones));

        List<Product> result = productService.findAll();

        assertThat(result).hasSize(1).contains(headphones);
    }

    @Test
    void findById_returnsProduct_whenExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(headphones));

        Product result = productService.findById(1L);

        assertThat(result.getName()).isEqualTo("Wireless Headphones");
    }

    @Test
    void findById_throwsProductNotFound_whenMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void search_isCaseInsensitive() {
        when(productRepository.findByNameContainingIgnoreCase("phone")).thenReturn(List.of(headphones));

        List<Product> result = productService.search("phone");

        assertThat(result).containsExactly(headphones);
    }

    @Test
    void search_withBlankQuery_returnsAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(headphones));

        List<Product> result = productService.search("   ");

        assertThat(result).containsExactly(headphones);
    }
}
