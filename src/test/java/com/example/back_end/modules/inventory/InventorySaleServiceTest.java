package com.example.back_end.modules.inventory;

import com.example.back_end.exception.BusinessRuleException;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.inventory.service.InventorySaleService;
import com.example.back_end.modules.sales.order.entity.OrderItem;
import com.example.back_end.modules.store_product.entity.StockSnapshot;
import com.example.back_end.modules.store_product.repository.InventoryMovementRepository;
import com.example.back_end.modules.store_product.repository.StockSnapshotRepository;
import com.example.back_end.modules.stock.entity.InventoryMovement;
import com.example.back_end.modules.stock.enums.InventoryRefType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventorySaleServiceTest {

    @Test
    void applySaleOrder_decreasesStockAndCreatesMovement() {
        StockSnapshotRepository snapshotRepo = mock(StockSnapshotRepository.class);
        InventoryMovementRepository movementRepo = mock(InventoryMovementRepository.class);

        InventorySaleService service = new InventorySaleService(snapshotRepo, movementRepo);

        Product p = new Product();
        p.setId(10L);
        p.setName("Tea");

        OrderItem item = new OrderItem();
        item.setProduct(p);
        item.setQuantity(new BigDecimal("2"));

        StockSnapshot snap = StockSnapshot.builder()
                .productId(10L)
                .storeQty(new BigDecimal("5"))
                .warehouseQty(BigDecimal.ZERO)
                .lastUpdatedAt(Instant.now())
                .build();

        when(snapshotRepo.findByProductIdForUpdate(10L)).thenReturn(Optional.of(snap));

        service.applySaleOrder(100L, List.of(item));

        assertThat(snap.getStoreQty()).isEqualByComparingTo(new BigDecimal("3"));
        verify(snapshotRepo).save(snap);

        verify(movementRepo).save(argThat(m ->
                m.getProduct().getId().equals(10L)
                        && m.getRefType() == InventoryRefType.SALE
                        && m.getRefId().equals(100L)
                        && m.getQtyChange().compareTo(new BigDecimal("-2")) == 0
        ));
    }

    @Test
    void applySaleOrder_outOfStock_rollsBackByThrowing() {
        StockSnapshotRepository snapshotRepo = mock(StockSnapshotRepository.class);
        InventoryMovementRepository movementRepo = mock(InventoryMovementRepository.class);

        InventorySaleService service = new InventorySaleService(snapshotRepo, movementRepo);

        Product p = new Product();
        p.setId(10L);

        OrderItem item = new OrderItem();
        item.setProduct(p);
        item.setQuantity(new BigDecimal("2"));

        StockSnapshot snap = StockSnapshot.builder()
                .productId(10L)
                .storeQty(new BigDecimal("1"))
                .warehouseQty(BigDecimal.ZERO)
                .lastUpdatedAt(Instant.now())
                .build();

        when(snapshotRepo.findByProductIdForUpdate(10L)).thenReturn(Optional.of(snap));

        assertThatThrownBy(() -> service.applySaleOrder(100L, List.of(item)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("OUT_OF_STOCK");

        verify(movementRepo, never()).save(any(InventoryMovement.class));
        verify(snapshotRepo, never()).save(any(StockSnapshot.class));
    }
}

