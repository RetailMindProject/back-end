package com.example.back_end.modules.store_product.service;

import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import com.example.back_end.modules.store_product.dto.AdjustQuantityDTO;
import com.example.back_end.modules.store_product.dto.StoreProductResponseDTO;
import com.example.back_end.modules.store_product.dto.StoreTransferRequestDTO;
import com.example.back_end.modules.store_product.entity.StockSnapshot;
import com.example.back_end.modules.store_product.mapper.StoreProductMapper;
import com.example.back_end.modules.store_product.repository.InventoryMovementRepository;
import com.example.back_end.modules.store_product.repository.StockSnapshotRepository;
import com.example.back_end.modules.stock.entity.InventoryBatch;
import com.example.back_end.modules.stock.entity.InventoryMovement;
import com.example.back_end.modules.stock.entity.InventoryMovementBatch;
import com.example.back_end.modules.stock.entity.InventoryMovementBatchId;
import com.example.back_end.modules.stock.enums.InventoryLocationType;
import com.example.back_end.modules.stock.enums.InventoryRefType;
import com.example.back_end.modules.stock.repository.InventoryBatchRepository;
import com.example.back_end.modules.stock.repository.InventoryMovementBatchRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StoreProductServiceImpl implements StoreProductService {

    private final ProductRepository productRepository;
    private final StockSnapshotRepository snapshotRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryBatchRepository batchRepository;
    private final InventoryMovementBatchRepository movementBatchRepository;

    @Override
    public StoreProductResponseDTO addToInventory(StoreTransferRequestDTO dto) {
        log.info("=== addToInventory called ===");
        log.info("Received DTO - productId: {}, quantity: {}, expirationDate: {}, unitCost: {}", 
                dto.getProductId(), dto.getQuantity(), dto.getExpirationDate(), dto.getUnitCost());
        
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + dto.getProductId()));

        BigDecimal qty = dto.getQuantity();
        log.info("Extracted quantity from DTO: {} (scale: {}, precision: {})", 
                qty, qty != null ? qty.scale() : "null", qty != null ? qty.precision() : "null");
        
        if (qty == null || qty.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        BigDecimal unitCost = dto.getUnitCost() != null ? dto.getUnitCost() : 
                (product.getDefaultCost() != null ? product.getDefaultCost() : BigDecimal.ZERO);

        // load / create snapshot
        StockSnapshot snapshot = getOrCreateSnapshot(product.getId());
        BigDecimal currentWarehouseQty = nvl(snapshot.getWarehouseQty());

        // Always create a new movement (for audit trail)
        log.info("Creating new inventory movement with qtyChange: {}", qty);
        InventoryMovement purchaseMove = InventoryMovement.builder()
                .product(product)
                .locationType(InventoryLocationType.WAREHOUSE)
                .refType(InventoryRefType.PURCHASE)
                .qtyChange(qty)
                .unitCost(unitCost)
                .note(dto.getNote())
                .build();
        purchaseMove = movementRepository.save(purchaseMove);
        log.info("Saved inventory movement ID: {}, qtyChange in DB: {}", purchaseMove.getId(), purchaseMove.getQtyChange());

        // Handle batch: merge in inventory_batches, but record every movement in inventory_movement_batches
        if (dto.getExpirationDate() != null) {
            // Check if batch with this expiration date already exists
            InventoryBatch batch = batchRepository.findFirstByProductIdAndExpirationDate(
                    product.getId(), dto.getExpirationDate()).orElse(null);

            if (batch != null) {
                // Batch exists - merge: use existing batch, but create new movement_batch record
                log.info("Found existing batch ID: {} with expirationDate: {}, linking new movement ID: {} with qty: {}",
                        batch.getId(), dto.getExpirationDate(), purchaseMove.getId(), qty);
            } else {
                // Create new batch
                log.info("Creating new batch with expirationDate: {}, qty: {}", dto.getExpirationDate(), qty);
                batch = InventoryBatch.builder()
                        .product(product)
                        .expirationDate(dto.getExpirationDate())
                        .build();
                batch = batchRepository.save(batch);
                log.info("Saved new batch ID: {}", batch.getId());
            }

            // Always create a new movement_batch record (audit trail - records every movement)
            InventoryMovementBatchId movementBatchId = new InventoryMovementBatchId(batch.getId(), purchaseMove.getId());
            InventoryMovementBatch movementBatch = InventoryMovementBatch.builder()
                    .id(movementBatchId)
                    .batch(batch)
                    .inventoryMovement(purchaseMove)
                    .qty(qty)
                    .build();
            movementBatchRepository.save(movementBatch);
            log.info("Saved movement-batch link - batchId: {}, movementId: {}, qty: {} (new record for audit trail)", 
                    batch.getId(), purchaseMove.getId(), movementBatch.getQty());
        }

        // update snapshot
        BigDecimal newWarehouseQty = currentWarehouseQty.add(qty);
        log.info("Updating snapshot - current: {}, adding: {}, new: {}", 
                currentWarehouseQty, qty, newWarehouseQty);
        snapshot.setWarehouseQty(newWarehouseQty);
        snapshot.setLastUpdatedAt(Instant.now());
        snapshot = snapshotRepository.save(snapshot);
        log.info("Saved snapshot - warehouseQty: {}", snapshot.getWarehouseQty());
        log.info("=== addToInventory completed ===");

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

        // create TRANSFER movements (warehouse out, store in)
        InventoryMovement warehouseOut = InventoryMovement.builder()
                .product(product)
                .locationType(InventoryLocationType.WAREHOUSE)
                .refType(InventoryRefType.TRANSFER)
                .qtyChange(qty.negate())
                .unitCost(unitCost)
                .note(dto.getNote())
                .build();
        warehouseOut = movementRepository.save(warehouseOut);

        InventoryMovement storeIn = InventoryMovement.builder()
                .product(product)
                .locationType(InventoryLocationType.STORE)
                .refType(InventoryRefType.TRANSFER)
                .qtyChange(qty)
                .unitCost(unitCost)
                .note(dto.getNote())
                .build();
        movementRepository.save(storeIn);

        // If product has batches, allocate quantity using FIFO (First Expired First Out)
        List<InventoryBatch> batches = batchRepository.findByProductId(product.getId());
        if (!batches.isEmpty()) {
            // Get batches with remaining quantities, sorted by expiration date (earliest first)
            List<BatchWithRemainingQty> batchesWithRemaining = batches.stream()
                    .map(batch -> {
                        final Long batchId = batch.getId();
                        
                        // Calculate total quantity in batch (sum of all movement_batches)
                        BigDecimal totalQty = movementBatchRepository.findByBatch_Id(batchId).stream()
                                .map(InventoryMovementBatch::getQty)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // Calculate wasted quantity for this batch
                        BigDecimal wastedQty = movementRepository.findByProductId(product.getId()).stream()
                                .filter(m -> m.getRefType() == InventoryRefType.WASTED)
                                .flatMap(m -> m.getBatches().stream())
                                .filter(mb -> mb.getBatch().getId().equals(batchId))
                                .map(InventoryMovementBatch::getQty)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // Calculate transferred out quantity (warehouse out transfers)
                        BigDecimal transferredOutQty = movementRepository.findByProductId(product.getId()).stream()
                                .filter(m -> m.getRefType() == InventoryRefType.TRANSFER 
                                        && m.getLocationType() == InventoryLocationType.WAREHOUSE
                                        && m.getQtyChange().signum() < 0) // Negative qtyChange means out
                                .flatMap(m -> m.getBatches().stream())
                                .filter(mb -> mb.getBatch().getId().equals(batchId))
                                .map(InventoryMovementBatch::getQty)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal remainingQty = totalQty.subtract(wastedQty).subtract(transferredOutQty);
                        
                        return new BatchWithRemainingQty(batch, remainingQty);
                    })
                    .filter(b -> b.remainingQty.compareTo(BigDecimal.ZERO) > 0) // Only batches with remaining quantity
                    .sorted(Comparator.comparing(b -> b.batch.getExpirationDate(), 
                            Comparator.nullsLast(Comparator.naturalOrder()))) // Sort by expiration date (earliest first, nulls last)
                    .collect(Collectors.toList());

            // Allocate quantity using FIFO
            BigDecimal remainingToAllocate = qty;
            for (BatchWithRemainingQty batchInfo : batchesWithRemaining) {
                if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal qtyToAllocateFromBatch = remainingToAllocate.min(batchInfo.remainingQty);
                
                // Link warehouse out movement to this batch
                InventoryMovementBatchId movementBatchId = new InventoryMovementBatchId(
                        batchInfo.batch.getId(), warehouseOut.getId());
                InventoryMovementBatch movementBatch = InventoryMovementBatch.builder()
                        .id(movementBatchId)
                        .batch(batchInfo.batch)
                        .inventoryMovement(warehouseOut)
                        .qty(qtyToAllocateFromBatch) // Positive quantity for the batch link
                        .build();
                movementBatchRepository.save(movementBatch);
                log.info("Linked warehouse out movement {} to batch {} (expiration: {}) with qty: {}",
                        warehouseOut.getId(), batchInfo.batch.getId(), 
                        batchInfo.batch.getExpirationDate(), qtyToAllocateFromBatch);

                remainingToAllocate = remainingToAllocate.subtract(qtyToAllocateFromBatch);
            }

            if (remainingToAllocate.compareTo(BigDecimal.ZERO) > 0) {
                log.warn("Could not fully allocate transfer quantity {} to batches. Remaining unallocated: {}", 
                        qty, remainingToAllocate);
                // This shouldn't happen if validation is correct, but log it just in case
            }
        }

        // update snapshot
        snapshot.setWarehouseQty(availableWarehouse.subtract(qty));
        snapshot.setStoreQty(nvl(snapshot.getStoreQty()).add(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);

        return StoreProductMapper.fromSnapshotForTransfer(product, snapshot);
    }

    // Helper class for batch allocation
    private static class BatchWithRemainingQty {
        InventoryBatch batch;
        BigDecimal remainingQty;

        BatchWithRemainingQty(InventoryBatch batch, BigDecimal remainingQty) {
            this.batch = batch;
            this.remainingQty = remainingQty;
        }
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

        // create TRANSFER movements (store out, warehouse in)
        InventoryMovement storeOut = InventoryMovement.builder()
                .product(product)
                .locationType(InventoryLocationType.STORE)
                .refType(InventoryRefType.TRANSFER)
                .qtyChange(qty.negate())
                .unitCost(unitCost)
                .note(dto.getNote())
                .build();
        movementRepository.save(storeOut);

        InventoryMovement warehouseIn = InventoryMovement.builder()
                .product(product)
                .locationType(InventoryLocationType.WAREHOUSE)
                .refType(InventoryRefType.TRANSFER)
                .qtyChange(qty)
                .unitCost(unitCost)
                .note(dto.getNote())
                .build();
        movementRepository.save(warehouseIn);

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

        // create ADJUSTMENT movement
        InventoryMovement adjustment = InventoryMovement.builder()
                .product(product)
                .locationType(InventoryLocationType.STORE)
                .refType(InventoryRefType.ADJUSTMENT)
                .qtyChange(qty)
                .note(dto.getNote())
                .build();
        movementRepository.save(adjustment);

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

        // create ADJUSTMENT movement (negative)
        InventoryMovement adjustment = InventoryMovement.builder()
                .product(product)
                .locationType(InventoryLocationType.STORE)
                .refType(InventoryRefType.ADJUSTMENT)
                .qtyChange(qty.negate())
                .note(dto.getNote())
                .build();
        movementRepository.save(adjustment);

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

        // create ADJUSTMENT movement
        InventoryMovement adjustment = InventoryMovement.builder()
                .product(product)
                .locationType(InventoryLocationType.WAREHOUSE)
                .refType(InventoryRefType.ADJUSTMENT)
                .qtyChange(qty)
                .note(dto.getNote())
                .build();
        movementRepository.save(adjustment);

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

        // create ADJUSTMENT movement (negative)
        InventoryMovement adjustment = InventoryMovement.builder()
                .product(product)
                .locationType(InventoryLocationType.WAREHOUSE)
                .refType(InventoryRefType.ADJUSTMENT)
                .qtyChange(qty.negate())
                .note(dto.getNote())
                .build();
        movementRepository.save(adjustment);

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

    @Override
    @Transactional(readOnly = true)
    public Page<StoreProductResponseDTO> getProductsWithInventory(String q, Pageable pageable) {
        return snapshotRepository.findProductsWithInventory(q, pageable)
                .map(StoreProductMapper::fromProjection);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.example.back_end.modules.store_product.dto.ProductBatchDTO> getBatchesForProduct(Long productId) {
        // Verify product exists
        productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        // Get all batches for the product
        java.util.List<InventoryBatch> batches = batchRepository.findByProductId(productId);

        // Calculate total quantity, wasted quantity, and transferred out quantity for each batch
        return batches.stream()
                .map(batch -> {
                    final Long batchId = batch.getId();
                    
                    // Calculate total purchased/added quantity (only positive movements like PURCHASE)
                    // This is the correct total - we should NOT include wasted/transferred quantities in the total
                    BigDecimal totalQty = movementBatchRepository.sumPurchasedQuantityByBatchId(batchId);

                    // Calculate wasted quantity for this batch (using direct SQL query for accuracy)
                    BigDecimal wastedQty = movementBatchRepository.sumWastedQuantityByBatchId(batchId);

                    // Calculate transferred out quantity (warehouse out transfers) using direct SQL query
                    BigDecimal transferredOutQty = movementBatchRepository.sumTransferredOutQuantityByBatchId(batchId);

                    // Calculate remaining quantity (purchased - wasted - transferred out)
                    BigDecimal remainingQty = totalQty.subtract(wastedQty).subtract(transferredOutQty);

                    // Log batch details for debugging (INFO level to ensure it's visible)
                    log.info("getBatchesForProduct - Batch {} (expiration: {}) - totalQty: {}, wastedQty: {}, transferredOutQty: {}, remainingQty: {}",
                            batchId, batch.getExpirationDate(), totalQty, wastedQty, transferredOutQty, remainingQty);

                    return com.example.back_end.modules.store_product.dto.ProductBatchDTO.builder()
                            .batchId(batch.getId())
                            .expirationDate(batch.getExpirationDate())
                            .totalQuantity(remainingQty) // Return remaining quantity in warehouse
                            .build();
                })
                .peek(batch -> log.info("getBatchesForProduct - Before filter: batch {} with totalQuantity: {}", 
                        batch.getBatchId(), batch.getTotalQuantity()))
                .filter(batch -> {
                    boolean shouldInclude = batch.getTotalQuantity().compareTo(BigDecimal.ZERO) > 0;
                    log.info("getBatchesForProduct - Batch {} (expiration: {}) - totalQuantity: {}, shouldInclude: {}",
                            batch.getBatchId(), batch.getExpirationDate(), batch.getTotalQuantity(), shouldInclude);
                    return shouldInclude;
                }) // Only batches with remaining quantity > 0
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public StoreProductResponseDTO restock(StoreTransferRequestDTO dto) {
        log.info("=== restock called ===");
        log.info("Received DTO - productId: {}, quantity: {}, expirationDate: {}, unitCost: {}",
                dto.getProductId(), dto.getQuantity(), dto.getExpirationDate(), dto.getUnitCost());

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + dto.getProductId()));

        // Verify product has existing inventory
        StockSnapshot existingSnapshot = snapshotRepository.findById(product.getId()).orElse(null);
        if (existingSnapshot == null || 
            (nvl(existingSnapshot.getWarehouseQty()).signum() <= 0 && 
             nvl(existingSnapshot.getStoreQty()).signum() <= 0)) {
            throw new IllegalArgumentException("Product must have existing inventory to re-stock. Use add-to-inventory for new products.");
        }

        BigDecimal qty = dto.getQuantity();
        if (qty == null || qty.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        log.info("Extracted quantity from DTO: {} (scale: {}, precision: {})",
                qty, qty.scale(), qty.precision());

        BigDecimal unitCost = dto.getUnitCost() != null ? dto.getUnitCost() :
                (product.getDefaultCost() != null ? product.getDefaultCost() : BigDecimal.ZERO);

        // load snapshot
        StockSnapshot snapshot = getOrCreateSnapshot(product.getId());
        BigDecimal currentWarehouseQty = nvl(snapshot.getWarehouseQty());

        // Always create a new movement (for audit trail)
        log.info("Creating new inventory movement with qtyChange: {}", qty);
        InventoryMovement purchaseMove = InventoryMovement.builder()
                .product(product)
                .locationType(InventoryLocationType.WAREHOUSE)
                .refType(InventoryRefType.PURCHASE)
                .qtyChange(qty)
                .unitCost(unitCost)
                .note(dto.getNote() != null ? dto.getNote() : "Re-stock")
                .build();
        purchaseMove = movementRepository.save(purchaseMove);
        log.info("Saved inventory movement ID: {}, qtyChange in DB: {}", purchaseMove.getId(), purchaseMove.getQtyChange());

        // Handle batch: merge in inventory_batches, but record every movement in inventory_movement_batches
        if (dto.getExpirationDate() != null) {
            // Check if batch with this expiration date already exists
            InventoryBatch batch = batchRepository.findFirstByProductIdAndExpirationDate(
                    product.getId(), dto.getExpirationDate()).orElse(null);

            if (batch != null) {
                // Batch exists - merge: use existing batch, but create new movement_batch record
                log.info("Found existing batch ID: {} with expirationDate: {}, linking new movement ID: {} with qty: {}",
                        batch.getId(), dto.getExpirationDate(), purchaseMove.getId(), qty);
            } else {
                // Create new batch
                log.info("Creating new batch with expirationDate: {}, qty: {}", dto.getExpirationDate(), qty);
                batch = InventoryBatch.builder()
                        .product(product)
                        .expirationDate(dto.getExpirationDate())
                        .build();
                batch = batchRepository.save(batch);
                log.info("Saved new batch ID: {}", batch.getId());
            }

            // Always create a new movement_batch record (audit trail - records every movement)
            InventoryMovementBatchId movementBatchId = new InventoryMovementBatchId(batch.getId(), purchaseMove.getId());
            InventoryMovementBatch movementBatch = InventoryMovementBatch.builder()
                    .id(movementBatchId)
                    .batch(batch)
                    .inventoryMovement(purchaseMove)
                    .qty(qty)
                    .build();
            movementBatchRepository.save(movementBatch);
            log.info("Saved movement-batch link - batchId: {}, movementId: {}, qty: {} (new record for audit trail)",
                    batch.getId(), purchaseMove.getId(), movementBatch.getQty());
        }

        // update snapshot
        snapshot.setWarehouseQty(currentWarehouseQty.add(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);
        log.info("Updated stock snapshot for product ID: {} - new warehouseQty: {}", product.getId(), snapshot.getWarehouseQty());

        return StoreProductMapper.fromSnapshotForTransfer(product, snapshot);
    }

    @Override
    public com.example.back_end.modules.store_product.dto.WasteResponseDTO recordWaste(
            com.example.back_end.modules.store_product.dto.WasteRequestDTO dto) {
        log.info("=== recordWaste called ===");
        log.info("Received DTO - productId: {}, batchId: {}, quantity: {}, note: {}",
                dto.getProductId(), dto.getBatchId(), dto.getQuantity(), dto.getNote());

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + dto.getProductId()));

        BigDecimal qty = dto.getQuantity();
        if (qty == null || qty.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        // Validate batch if provided
        InventoryBatch batch = null;
        if (dto.getBatchId() != null) {
            batch = batchRepository.findById(dto.getBatchId())
                    .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + dto.getBatchId()));

            if (!batch.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("Batch does not belong to the specified product");
            }

            // Check available quantity in batch
            final Long batchId = batch.getId(); // Make final for lambda
            BigDecimal batchTotalQty = movementBatchRepository.findByBatch_Id(batchId).stream()
                    .map(InventoryMovementBatch::getQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Subtract wasted quantities from batch total
            BigDecimal batchWastedQty = movementRepository.findByProductId(product.getId()).stream()
                    .filter(m -> m.getRefType() == InventoryRefType.WASTED)
                    .flatMap(m -> m.getBatches().stream())
                    .filter(mb -> mb.getBatch().getId().equals(batchId))
                    .map(InventoryMovementBatch::getQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal availableQty = batchTotalQty.subtract(batchWastedQty);
            if (qty.compareTo(availableQty) > 0) {
                throw new IllegalArgumentException(
                        String.format("Cannot waste %s units. Available quantity in batch: %s", qty, availableQty));
            }
        }

        // Load snapshot - waste is always from warehouse
        StockSnapshot snapshot = getOrCreateSnapshot(product.getId());
        BigDecimal currentQty = nvl(snapshot.getWarehouseQty());

        if (qty.compareTo(currentQty) > 0) {
            throw new IllegalArgumentException(
                    String.format("Cannot waste %s units. Available warehouse quantity: %s", qty, currentQty));
        }

        // Get unit cost
        BigDecimal unitCost = product.getDefaultCost() != null ? product.getDefaultCost() : BigDecimal.ZERO;

        // Create WASTED movement with negative qtyChange - always WAREHOUSE
        log.info("Creating WASTED movement with qtyChange: -{}, locationType: WAREHOUSE", qty);
        InventoryMovement wasteMove = InventoryMovement.builder()
                .product(product)
                .locationType(InventoryLocationType.WAREHOUSE) // Hardcoded to warehouse
                .refType(InventoryRefType.WASTED)
                .qtyChange(qty.negate()) // Negative quantity for waste
                .unitCost(unitCost)
                .note(dto.getNote()) // Store waste reason in note field
                .build();
        wasteMove = movementRepository.save(wasteMove);
        log.info("Saved waste movement ID: {}, qtyChange: {}", wasteMove.getId(), wasteMove.getQtyChange());

        // Link to batch if provided
        if (batch != null) {
            InventoryMovementBatchId movementBatchId = new InventoryMovementBatchId(batch.getId(), wasteMove.getId());
            InventoryMovementBatch movementBatch = InventoryMovementBatch.builder()
                    .id(movementBatchId)
                    .batch(batch)
                    .inventoryMovement(wasteMove)
                    .qty(qty) // Positive quantity for the batch link
                    .build();
            movementBatchRepository.save(movementBatch);
            log.info("Saved movement-batch link - batchId: {}, movementId: {}, qty: {}",
                    batch.getId(), wasteMove.getId(), movementBatch.getQty());
        }

        // Update snapshot - always decrease warehouse quantity
        snapshot.setWarehouseQty(currentQty.subtract(qty));
        snapshot.setLastUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);
        log.info("Updated stock snapshot - WAREHOUSE qty: {}", snapshot.getWarehouseQty());

        // Build response
        return com.example.back_end.modules.store_product.dto.WasteResponseDTO.builder()
                .movementId(wasteMove.getId())
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .batchId(batch != null ? batch.getId() : null)
                .expirationDate(batch != null ? batch.getExpirationDate() : null)
                .wastedQuantity(qty)
                .wasteReason(dto.getNote())
                .unitCost(unitCost)
                .totalCost(qty.multiply(unitCost))
                .locationType(InventoryLocationType.WAREHOUSE) // Always warehouse
                .wastedAt(wasteMove.getMovedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.example.back_end.modules.store_product.dto.WasteHistoryDTO> getWasteHistory(
            Long productId, Long batchId, Instant fromDate, Instant toDate, String wasteReason, Pageable pageable) {
        
        Page<com.example.back_end.modules.stock.repository.projection.WasteHistoryProjection> projections = 
                movementRepository.findWasteHistory(productId, batchId, fromDate, toDate, wasteReason, pageable);

        return projections.map(proj -> com.example.back_end.modules.store_product.dto.WasteHistoryDTO.builder()
                .movementId(proj.getMovementId())
                .productId(proj.getProductId())
                .productName(proj.getProductName())
                .sku(proj.getSku())
                .batchId(proj.getBatchId())
                .expirationDate(proj.getExpirationDate())
                .quantity(proj.getQuantity())
                .wasteReason(proj.getWasteReason())
                .note(proj.getNote())
                .unitCost(proj.getUnitCost())
                .totalCost(proj.getTotalCost())
                .locationType(InventoryLocationType.valueOf(proj.getLocationType()))
                .wastedAt(proj.getWastedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreProductResponseDTO> getWastedProducts(String q, Pageable pageable) {
        return snapshotRepository.findWastedProducts(q, pageable)
                .map(StoreProductMapper::fromProjection);
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
