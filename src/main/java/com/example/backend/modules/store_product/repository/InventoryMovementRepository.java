package com.example.backend.modules.store_product.repository;

import com.example.backend.modules.store_product.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
}
