package com.giovanni.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giovanni.orderhub.dto.product.CreateProductRequest;
import com.giovanni.orderhub.dto.product.ProductResponse;
import com.giovanni.orderhub.exception.DuplicateSkuException;
import com.giovanni.orderhub.exception.InsufficientStockException;
import com.giovanni.orderhub.exception.ProductNotFoundException;
import com.giovanni.orderhub.model.Product;
import com.giovanni.orderhub.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProductWhenSkuIsNew() {
        CreateProductRequest request = new CreateProductRequest("CAM-001", "Camiseta", new BigDecimal("59.90"), 10);
        when(productRepository.existsBySku("CAM-001")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.create(request);

        assertThat(response.sku()).isEqualTo("CAM-001");
        assertThat(response.stockQuantity()).isEqualTo(10);
    }

    @Test
    void shouldRejectDuplicateSku() {
        CreateProductRequest request = new CreateProductRequest("CAM-001", "Camiseta", new BigDecimal("59.90"), 10);
        when(productRepository.existsBySku("CAM-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request)).isInstanceOf(DuplicateSkuException.class);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowNotFoundWhenSkuDoesNotExist() {
        when(productRepository.findBySku("NAO-EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findBySku("NAO-EXISTE"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldNotSaveWhenStockAdjustmentWouldBeNegative() {
        Product product = new Product("CAM-001", "Camiseta", new BigDecimal("59.90"), 2);
        when(productRepository.findBySku("CAM-001")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.adjustStock("CAM-001", -5))
                .isInstanceOf(InsufficientStockException.class);
        verify(productRepository, never()).saveAndFlush(any());
    }
}
