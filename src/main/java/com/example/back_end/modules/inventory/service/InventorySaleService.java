package com.example.back_end.modules.inventory.service;

import com.example.back_end.exception.BusinessRuleException;
import com.example.back_end.modules.sales.order.entity.OrderItem;
import com.example.back_end.modules.store_product.entity.StockSnapshot;
import com.example.back_end.modules.store_product.repository.InventoryMovementRepository;
import com.example.back_end.modules.store_product.repository.StockSnapshotRepository;
import com.example.back_end.modules.stock.entity.InventoryMovement;
import com.example.back_end.modules.stock.enums.InventoryLocationType;
import com.example.back_end.modules.stock.enums.InventoryRefType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventorySaleService {

    private final StockSnapshotRepository stockSnapshotRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final LowStockAlertService lowStockAlertService;

    /**
     * Apply inventory effects of a SALE order: decrement store stock + insert movements.
     *
     * Contract:
     * - Must be called after the Order is persisted (orderId exists)
     * - All operations are transactional: snapshot updates + movements persist together
     * - Concurrency safe: locks stock_snapshot rows per product
     * - Optional no-negative-stock validation: throws BusinessRuleException when insufficient
     */
    @Transactional
    public void applySaleOrder(Long orderId, List<OrderItem> items) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (items == null || items.isEmpty()) {
            return; // nothing to do
        }

        // Aggregate quantities per productId to avoid double-locking and duplicate snapshot updates.
        Map<Long, BigDecimal> qtyByProductId = items.stream()
                .filter(i -> i.getProduct() != null && i.getProduct().getId() != null)
                .collect(Collectors.groupingBy(i -> i.getProduct().getId(),
                        Collectors.mapping(OrderItem::getQuantity,
                                Collectors.reducing(BigDecimal.ZERO, this::nvlAdd))));

        Map<Long, com.example.back_end.modules.catalog.product.entity.Product> productById = items.stream()
                .filter(i -> i.getProduct() != null && i.getProduct().getId() != null)
                .collect(Collectors.toMap(i -> i.getProduct().getId(), OrderItem::getProduct, (a, b) -> a));

        for (Map.Entry<Long, BigDecimal> e : qtyByProductId.entrySet()) {
            Long productId = e.getKey();
            BigDecimal soldQty = nvl(e.getValue());
            if (soldQty.signum() <= 0) {
                continue;
            }

            StockSnapshot snapshot = lockOrCreateSnapshot(productId);

            BigDecimal available = nvl(snapshot.getStoreQty());
            if (available.compareTo(soldQty) < 0) {
                log.warn("Out of stock: productId={}, available={}, requested={}, orderId={}", productId, available, soldQty, orderId);
                throw new BusinessRuleException("OUT_OF_STOCK: productId=" + productId + ", available=" + available + ", requested=" + soldQty);
            }

            com.example.back_end.modules.catalog.product.entity.Product product = productById.get(productId);
            if (product == null) {
                throw new IllegalStateException("Missing Product reference for productId=" + productId);
            }

            // Create movement (audit trail)
            InventoryMovement movement = InventoryMovement.builder()
                    .product(product)
                    .locationType(InventoryLocationType.STORE)
                    .refType(InventoryRefType.SALE)
                    .refId(orderId)
                    .qtyChange(soldQty.negate())
                    .unitCost(null)
                    .movedAt(Instant.now())
                    .note("Sale from cashier order " + orderId)
                    .build();
            inventoryMovementRepository.save(movement);

            // Update snapshot
            BigDecimal prevQty = available;
            BigDecimal newQty = available.subtract(soldQty);
            snapshot.setStoreQty(newQty);
            snapshot.setLastUpdatedAt(Instant.now());
            stockSnapshotRepository.save(snapshot);

            // Low-stock alert (crossing threshold only + duplicate check)
            lowStockAlertService.handleLowStock(product, prevQty, newQty, orderId);
        }
    }

    private StockSnapshot lockOrCreateSnapshot(Long productId) {
        // Lock row if exists
        StockSnapshot snapshot = stockSnapshotRepository.findByProductIdForUpdate(productId).orElse(null);
        if (snapshot != null) {
            return snapshot;
        }

        // Not found: create + then lock again.
        try {
            StockSnapshot created = StockSnapshot.builder()
                    .productId(productId)
                    .storeQty(BigDecimal.ZERO)
                    .warehouseQty(BigDecimal.ZERO)
                    .lastUpdatedAt(Instant.now())
                    .build();
            stockSnapshotRepository.saveAndFlush(created);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert happened, ignore and lock below.
            log.debug("StockSnapshot insert race for productId={}, will re-fetch with lock", productId);
        }

        return stockSnapshotRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("Failed to create stock snapshot for productId=" + productId));
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal nvlAdd(BigDecimal a, BigDecimal b) {
        return nvl(a).add(nvl(b));
    }
}
