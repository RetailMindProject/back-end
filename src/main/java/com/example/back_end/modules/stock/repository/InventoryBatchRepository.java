package com.example.back_end.modules.stock.repository;

import com.example.back_end.modules.stock.entity.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    List<InventoryBatch> findByProductId(Long productId);

    List<InventoryBatch> findByProductIdAndExpirationDate(Long productId, LocalDate expirationDate);

    Optional<InventoryBatch> findFirstByProductIdAndExpirationDate(Long productId, LocalDate expirationDate);

    List<InventoryBatch> findByExpirationDateBefore(LocalDate date);

    List<InventoryBatch> findByExpirationDateBetween(LocalDate startDate, LocalDate endDate);
}

