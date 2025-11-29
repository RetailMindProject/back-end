package com.example.backend.modules.store_product.service;

import com.example.backend.modules.catalog.product.entity.Product;
import com.example.backend.modules.catalog.product.repository.ProductRepository;
import com.example.backend.modules.store_product.dto.StoreProductResponseDTO;
import com.example.backend.modules.store_product.dto.StoreTransferRequestDTO;
import com.example.backend.modules.store_product.entity.InventoryMovement;
import com.example.backend.modules.store_product.entity.StockSnapshot;
import com.example.backend.modules.store_product.mapper.StoreProductMapper;
import com.example.backend.modules.store_product.repository.InventoryMovementRepository;
import com.example.backend.modules.store_product.repository.StockSnapshotRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreProductServiceImpl implements StoreProductService {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;
    private final StockSnapshotRepository snapshotRepository;

    private static final String LOCATION_WAREHOUSE = "WAREHOUSE";
    private static final String LOCATION_STORE = "STORE";
    private static final String REF_TRANSFER = "TRANSFER";

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

        BigDecimal unitCost = dto.getUnitCost() != null ? dto.getUnitCost() : product.getDefaultCost();

        // warehouse OUT
        InventoryMovement whMove = InventoryMovement.builder()
                .product(product)
                .locationType(LOCATION_WAREHOUSE)
                .refType(REF_TRANSFER)
                .qtyChange(qty.negate())
                .unitCost(unitCost)
                .note(dto.getNote())
                .build();

        // store IN
        InventoryMovement storeMove = InventoryMovement.builder()
                .product(product)
                .locationType(LOCATION_STORE)
                .refType(REF_TRANSFER)
                .qtyChange(qty)
                .unitCost(unitCost)
                .note(dto.getNote())
                .build();

        movementRepository.save(whMove);
        movementRepository.save(storeMove);

        // update snapshot
        snapshot.setWarehouseQty(availableWarehouse.subtract(qty));
        snapshot.setStoreQty(nvl(snapshot.getStoreQty()).add(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);

        return StoreProductMapper.fromSnapshot(product, snapshot);
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

        BigDecimal unitCost = dto.getUnitCost() != null ? dto.getUnitCost() : product.getDefaultCost();

        // store OUT
        InventoryMovement storeMove = InventoryMovement.builder()
                .product(product)
                .locationType(LOCATION_STORE)
                .refType(REF_TRANSFER)
                .qtyChange(qty.negate())
                .unitCost(unitCost)
                .note(dto.getNote())
                .build();

        // warehouse IN
        InventoryMovement whMove = InventoryMovement.builder()
                .product(product)
                .locationType(LOCATION_WAREHOUSE)
                .refType(REF_TRANSFER)
                .qtyChange(qty)
                .unitCost(unitCost)
                .note(dto.getNote())
                .build();

        movementRepository.save(storeMove);
        movementRepository.save(whMove);

        // update snapshot
        snapshot.setStoreQty(availableStore.subtract(qty));
        snapshot.setWarehouseQty(nvl(snapshot.getWarehouseQty()).add(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);

        return StoreProductMapper.fromSnapshot(product, snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreProductResponseDTO> search(String q) {
        return snapshotRepository.searchStore(q).stream()
                .map(StoreProductMapper::fromProjection)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StoreProductResponseDTO getByProductId(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        StockSnapshot snapshot = snapshotRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Stock snapshot not found for product: " + productId));

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
