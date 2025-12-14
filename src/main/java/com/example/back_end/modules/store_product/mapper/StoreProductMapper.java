package com.example.back_end.modules.store_product.mapper;

import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.store_product.dto.StoreProductResponseDTO;
import com.example.back_end.modules.store_product.entity.StockSnapshot;

import java.math.BigDecimal;
import java.time.Instant;

public final class StoreProductMapper {

    private StoreProductMapper() {
    }

    public static StoreProductResponseDTO fromSnapshot(Product product, StockSnapshot snapshot) {
        if (product == null || snapshot == null) return null;

        return StoreProductResponseDTO.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .storeQty(nvl(snapshot.getStoreQty()))
                .warehouseQty(nvl(snapshot.getWarehouseQty()))
                .defaultPrice(product.getDefaultPrice())
                .defaultCost(product.getDefaultCost())
                .lastUpdatedAt(snapshot.getLastUpdatedAt())
                .build();
    }

    // For transfer operations - without price information
    public static StoreProductResponseDTO fromSnapshotForTransfer(Product product, StockSnapshot snapshot) {
        if (product == null || snapshot == null) return null;

        return StoreProductResponseDTO.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .storeQty(nvl(snapshot.getStoreQty()))
                .warehouseQty(nvl(snapshot.getWarehouseQty()))
                .lastUpdatedAt(snapshot.getLastUpdatedAt())
                .build();
    }

    public static StoreProductResponseDTO fromProjection(StockProjection p) {
        if (p == null) return null;

        return StoreProductResponseDTO.builder()
                .productId(p.getProductId())
                .sku(p.getSku())
                .name(p.getName())
                .storeQty(nvl(p.getStoreQty()))
                .warehouseQty(nvl(p.getWarehouseQty()))
                .defaultPrice(p.getDefaultPrice())
                .defaultCost(p.getDefaultCost())
                .lastUpdatedAt(p.getLastUpdatedAt())
                .build();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // Projection interface used by repository native query
    public interface StockProjection {
        Long getProductId();
        String getSku();
        String getName();
        BigDecimal getStoreQty();
        BigDecimal getWarehouseQty();
        BigDecimal getDefaultPrice();
        BigDecimal getDefaultCost();
        Instant getLastUpdatedAt();
    }
}
