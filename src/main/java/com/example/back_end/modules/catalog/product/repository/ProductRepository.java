package com.example.back_end.modules.catalog.product.repository;

import com.example.back_end.modules.catalog.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
