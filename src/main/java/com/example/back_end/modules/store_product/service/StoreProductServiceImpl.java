package com.example.back_end.modules.store_product.service;

import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import com.example.back_end.modules.store_product.dto.AdjustQuantityDTO;
import com.example.back_end.modules.store_product.dto.StoreProductResponseDTO;
import com.example.back_end.modules.store_product.dto.StoreTransferRequestDTO;
import com.example.back_end.modules.stock.enums.InventoryLocationType;
import com.example.back_end.modules.stock.enums.InventoryRefType;
import com.example.back_end.modules.store_product.entity.StockSnapshot;
import com.example.back_end.modules.store_product.mapper.StoreProductMapper;
import com.example.back_end.modules.store_product.repository.StockSnapshotRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreProductServiceImpl implements StoreProductService {

    private final ProductRepository productRepository;
    private final StockSnapshotRepository snapshotRepository;

    @Override
    public StoreProductResponseDTO addToInventory(StoreTransferRequestDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + dto.getProductId()));

        BigDecimal qty = dto.getQuantity();
        if (qty == null || qty.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        BigDecimal unitCost = dto.getUnitCost() != null ? dto.getUnitCost() : 
                (product.getDefaultCost() != null ? product.getDefaultCost() : BigDecimal.ZERO);

        // load / create snapshot
        StockSnapshot snapshot = getOrCreateSnapshot(product.getId());
        BigDecimal currentWarehouseQty = nvl(snapshot.getWarehouseQty());

        // update snapshot
        snapshot.setWarehouseQty(currentWarehouseQty.add(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);

        return StoreProductMapper.fromSnapshotForTransfer(product, snapshot);
    }

    @Override
    public StoreProductResponseDTO transferFromInventoryToStore(StoreTransferRequestDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + dto.getProductId()));

        BigDecimal qty = dto.getQuantity();
        if (qty == null || qty.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        // load / create snapshot and validate warehouse has enough
        StockSnapshot snapshot = getOrCreateSnapshot(product.getId());
        BigDecimal availableWarehouse = nvl(snapshot.getWarehouseQty());

        if (availableWarehouse.compareTo(qty) < 0) {
            throw new IllegalArgumentException("Not enough quantity in warehouse. " +
                    "Available: " + availableWarehouse + ", requested: " + qty);
        }

        BigDecimal unitCost = dto.getUnitCost() != null ? dto.getUnitCost() : 
                (product.getDefaultCost() != null ? product.getDefaultCost() : BigDecimal.ZERO);


        // update snapshot
        snapshot.setWarehouseQty(availableWarehouse.subtract(qty));
        snapshot.setStoreQty(nvl(snapshot.getStoreQty()).add(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);

        return StoreProductMapper.fromSnapshotForTransfer(product, snapshot);
    }

    @Override
    public StoreProductResponseDTO transferFromStoreToInventory(StoreTransferRequestDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + dto.getProductId()));

        BigDecimal qty = dto.getQuantity();
        if (qty == null || qty.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        // load / create snapshot and validate store has enough
        StockSnapshot snapshot = getOrCreateSnapshot(product.getId());
        BigDecimal availableStore = nvl(snapshot.getStoreQty());

        if (availableStore.compareTo(qty) < 0) {
            throw new IllegalArgumentException("Not enough quantity in store. " +
                    "Available: " + availableStore + ", requested: " + qty);
        }

        BigDecimal unitCost = dto.getUnitCost() != null ? dto.getUnitCost() : 
                (product.getDefaultCost() != null ? product.getDefaultCost() : BigDecimal.ZERO);


        // update snapshot
        snapshot.setStoreQty(availableStore.subtract(qty));
        snapshot.setWarehouseQty(nvl(snapshot.getWarehouseQty()).add(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);

        return StoreProductMapper.fromSnapshotForTransfer(product, snapshot);
    }

    @Override
    public StoreProductResponseDTO removeFromStore(StoreTransferRequestDTO dto) {
        // Removing from store means transferring store → warehouse
        // Same operation as transferFromStoreToInventory
        return transferFromStoreToInventory(dto);
    }

    @Override
    public StoreProductResponseDTO increaseStoreQuantity(AdjustQuantityDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + dto.getProductId()));

        BigDecimal qty = dto.getQuantity();
        if (qty == null || qty.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        StockSnapshot snapshot = getOrCreateSnapshot(product.getId());
        BigDecimal currentStoreQty = nvl(snapshot.getStoreQty());


        // update snapshot
        snapshot.setStoreQty(currentStoreQty.add(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);

        return StoreProductMapper.fromSnapshotForTransfer(product, snapshot);
    }

    @Override
    public StoreProductResponseDTO decreaseStoreQuantity(AdjustQuantityDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + dto.getProductId()));

        BigDecimal qty = dto.getQuantity();
        if (qty == null || qty.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        StockSnapshot snapshot = getOrCreateSnapshot(product.getId());
        BigDecimal currentStoreQty = nvl(snapshot.getStoreQty());

        if (currentStoreQty.compareTo(qty) < 0) {
            throw new IllegalArgumentException("Not enough quantity in store. " +
                    "Available: " + currentStoreQty + ", requested: " + qty);
        }


        // update snapshot
        snapshot.setStoreQty(currentStoreQty.subtract(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);

        return StoreProductMapper.fromSnapshotForTransfer(product, snapshot);
    }

    @Override
    public StoreProductResponseDTO increaseWarehouseQuantity(AdjustQuantityDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + dto.getProductId()));

        BigDecimal qty = dto.getQuantity();
        if (qty == null || qty.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        StockSnapshot snapshot = getOrCreateSnapshot(product.getId());
        BigDecimal currentWarehouseQty = nvl(snapshot.getWarehouseQty());


        // update snapshot
        snapshot.setWarehouseQty(currentWarehouseQty.add(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);

        return StoreProductMapper.fromSnapshotForTransfer(product, snapshot);
    }

    @Override
    public StoreProductResponseDTO decreaseWarehouseQuantity(AdjustQuantityDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + dto.getProductId()));

        BigDecimal qty = dto.getQuantity();
        if (qty == null || qty.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        StockSnapshot snapshot = getOrCreateSnapshot(product.getId());
        BigDecimal currentWarehouseQty = nvl(snapshot.getWarehouseQty());

        if (currentWarehouseQty.compareTo(qty) < 0) {
            throw new IllegalArgumentException("Not enough quantity in warehouse. " +
                    "Available: " + currentWarehouseQty + ", requested: " + qty);
        }


        // update snapshot
        snapshot.setWarehouseQty(currentWarehouseQty.subtract(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);

        return StoreProductMapper.fromSnapshotForTransfer(product, snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreProductResponseDTO> search(String q, Pageable pageable) {
        // Normalize empty string to null
        String normalizedQ = (q != null && q.trim().isEmpty()) ? null : q;
        return snapshotRepository.searchStore(normalizedQ, pageable)
                .map(StoreProductMapper::fromProjection);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreProductResponseDTO> filter(String brand, Boolean isActive,
                                                BigDecimal minPrice, BigDecimal maxPrice,
                                                String sku, Pageable pageable) {
        // Validate price range
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
        }
        return snapshotRepository.filterStore(brand, isActive, minPrice, maxPrice, sku, pageable)
                .map(StoreProductMapper::fromProjection);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreProductResponseDTO getByProductId(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        StockSnapshot snapshot = getOrCreateSnapshot(productId);

        return StoreProductMapper.fromSnapshot(product, snapshot);
    }

    // helpers
    private StockSnapshot getOrCreateSnapshot(Long productId) {
        return snapshotRepository.findById(productId)
                .orElseGet(() -> StockSnapshot.builder()
                        .productId(productId)
                        .storeQty(BigDecimal.ZERO)
                        .warehouseQty(BigDecimal.ZERO)
                        .lastUpdatedAt(Instant.now())
                        .build()
                );
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
