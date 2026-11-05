package com.giovanni.orderhub.product;

import com.giovanni.orderhub.product.dto.CreateProductRequest;
import com.giovanni.orderhub.product.dto.ProductResponse;
import com.giovanni.orderhub.product.dto.StockAdjustmentRequest;
import com.giovanni.orderhub.product.dto.UpdateProductRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{sku}")
                .buildAndExpand(created.sku())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<ProductResponse> findAll(@PageableDefault(size = 20, sort = "sku") Pageable pageable) {
        return productService.findAll(pageable);
    }

    @GetMapping("/{sku}")
    public ProductResponse findBySku(@PathVariable String sku) {
        return productService.findBySku(sku);
    }

    @PutMapping("/{sku}")
    public ProductResponse update(@PathVariable String sku, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(sku, request);
    }

    @PatchMapping("/{sku}/stock")
    public ProductResponse adjustStock(@PathVariable String sku, @Valid @RequestBody StockAdjustmentRequest request) {
        return productService.adjustStock(sku, request.delta());
    }

    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> delete(@PathVariable String sku) {
        productService.delete(sku);
        return ResponseEntity.noContent().build();
    }
}
