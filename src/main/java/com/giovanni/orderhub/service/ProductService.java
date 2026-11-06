package com.giovanni.orderhub.service;

import com.giovanni.orderhub.dto.product.CreateProductRequest;
import com.giovanni.orderhub.dto.product.ProductResponse;
import com.giovanni.orderhub.dto.product.UpdateProductRequest;
import com.giovanni.orderhub.exception.DuplicateSkuException;
import com.giovanni.orderhub.exception.ProductNotFoundException;
import com.giovanni.orderhub.model.Product;
import com.giovanni.orderhub.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Product product = new Product(request.sku(), request.name(), request.price(), request.stockQuantity());
        Product saved = productRepository.saveAndFlush(product);
        log.info("Produto criado: sku={}", saved.getSku());
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse findBySku(String sku) {
        return ProductResponse.from(getProduct(sku));
    }

    @Transactional
    public ProductResponse update(String sku, UpdateProductRequest request) {
        Product product = getProduct(sku);
        product.updateDetails(request.name(), request.price());
        return ProductResponse.from(productRepository.saveAndFlush(product));
    }

    @Transactional
    public ProductResponse adjustStock(String sku, int delta) {
        Product product = getProduct(sku);
        product.adjustStock(delta);
        Product saved = productRepository.saveAndFlush(product);
        log.info("Estoque ajustado: sku={}, delta={}, novoEstoque={}", sku, delta, saved.getStockQuantity());
        return ProductResponse.from(saved);
    }

    @Transactional
    public void delete(String sku) {
        productRepository.delete(getProduct(sku));
        log.info("Produto removido: sku={}", sku);
    }

    private Product getProduct(String sku) {
        return productRepository.findBySku(sku).orElseThrow(() -> new ProductNotFoundException(sku));
    }
}
