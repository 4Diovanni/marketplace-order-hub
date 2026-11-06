package com.giovanni.orderhub.repository;

import com.giovanni.orderhub.model.Product;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findAllBySkuIn(Collection<String> skus);
}
