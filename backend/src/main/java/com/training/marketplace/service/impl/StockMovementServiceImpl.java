package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.CreateExportRequest;
import com.training.marketplace.dto.request.CreateImportRequest;
import com.training.marketplace.dto.request.CreateMovementItemRequest;
import com.training.marketplace.dto.request.CreateTransferRequest;
import com.training.marketplace.dto.response.MovementItemResponse;
import com.training.marketplace.dto.response.MovementResponse;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.entity.StockLevel;
import com.training.marketplace.entity.StockMovement;
import com.training.marketplace.entity.StockMovementItem;
import com.training.marketplace.entity.Warehouse;
import com.training.marketplace.enums.MovementStatus;
import com.training.marketplace.enums.MovementType;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.StockMovementMapper;
import com.training.marketplace.messaging.StockEventPublisher;
import com.training.marketplace.messaging.event.StockMovementCompletedEvent;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.repository.StockLevelRepository;
import com.training.marketplace.repository.StockMovementItemRepository;
import com.training.marketplace.repository.StockMovementRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.repository.WarehouseRepository;
import com.training.marketplace.service.StockCacheKey;
import com.training.marketplace.service.StockCacheService;
import com.training.marketplace.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockMovementServiceImpl implements StockMovementService {

    private static final int MAX_REFERENCE_ATTEMPTS = 5;
    private static final SecureRandom REFERENCE_RANDOM = new SecureRandom();

    private final StockMovementRepository stockMovementRepository;
    private final StockMovementItemRepository stockMovementItemRepository;
    private final StockLevelRepository stockLevelRepository;
    private final ProductVariantRepository variantRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final StockMovementMapper stockMovementMapper;
    private final StockCacheService stockCacheService;
    private final StockEventPublisher stockEventPublisher;

    @Override
    @Transactional
    public MovementResponse createImport(CreateImportRequest request) {
        log.debug("Creating import movement for warehouse id: {}", request.warehouseId());
        return createMovement(MovementType.IMPORT, request.warehouseId(), null, request.notes(), request.items());
    }

    @Override
    @Transactional
    public MovementResponse createExport(CreateExportRequest request) {
        log.debug("Creating export movement for warehouse id: {}", request.warehouseId());
        return createMovement(MovementType.EXPORT, request.warehouseId(), null, request.notes(), request.items());
    }

    @Override
    @Transactional
    public MovementResponse createTransfer(CreateTransferRequest request) {
        log.debug("Creating transfer movement {} -> {}", request.warehouseId(), request.destWarehouseId());
        return createMovement(MovementType.TRANSFER, request.warehouseId(),
                request.destWarehouseId(), request.notes(), request.items());
    }

    @Override
    @Transactional(readOnly = true)
    public MovementResponse getById(Long id) {
        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockMovement", id));
        return toResponse(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovementResponse> getAll(MovementType type, MovementStatus status, Long warehouseId, Pageable pageable) {
        return stockMovementRepository.findAllWithFilters(type, status, warehouseId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public MovementResponse approve(Long id) {
        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockMovement", id));
        if (movement.getStatus() != MovementStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only a PENDING_APPROVAL stock movement can be approved");
        }
        movement.setApprovedBy(resolveCurrentUserId());
        movement.setStatus(MovementStatus.APPROVED);
        StockMovement approved = stockMovementRepository.save(movement);
        log.info("Approved {} movement {} (id: {})", movement.getType(), movement.getReferenceNo(), movement.getId());
        return toResponse(approved);
    }

    @Override
    @Transactional
    public MovementResponse completeMovement(Long id) {
        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockMovement", id));
        if (movement.getStatus() != MovementStatus.APPROVED) {
            throw new BadRequestException("Only an APPROVED stock movement can be completed");
        }

        List<StockMovementItem> items = stockMovementItemRepository.findByMovementId(id);
        if (items.isEmpty()) {
            throw new BadRequestException("Stock movement must contain at least one item");
        }

        Map<Long, Integer> quantitiesByVariant = items.stream()
                .collect(Collectors.toMap(
                        StockMovementItem::getVariantId,
                        StockMovementItem::getQuantity,
                        Integer::sum,
                        TreeMap::new));
        List<Long> variantIds = List.copyOf(quantitiesByVariant.keySet());
        List<Long> warehouseIds = movementWarehouseIds(movement);

        Map<StockKey, StockLevel> levelsByKey = new java.util.HashMap<>();
        stockLevelRepository.findAllForUpdate(warehouseIds, variantIds)
                .forEach(level -> levelsByKey.put(
                        new StockKey(level.getWarehouseId(), level.getVariantId()), level));

        validateOutboundStock(movement, quantitiesByVariant, levelsByKey);
        applyStockChanges(movement, quantitiesByVariant, levelsByKey);

        List<StockLevel> changedLevels = new ArrayList<>(levelsByKey.values());
        changedLevels.sort(Comparator
                .comparing(StockLevel::getVariantId)
                .thenComparing(StockLevel::getWarehouseId));
        stockLevelRepository.saveAll(changedLevels);

        movement.setStatus(MovementStatus.COMPLETED);
        StockMovement completed = stockMovementRepository.save(movement);
        stockCacheService.evictAfterCommit(affectedCacheKeys(movement, variantIds));

        log.info("Completed {} movement {} (id: {})", movement.getType(), movement.getReferenceNo(), movement.getId());
        MovementResponse response = toResponse(completed);
        stockEventPublisher.publishMovementCompleted(
                StockMovementCompletedEvent.create(
                        completed.getId(), completed.getReferenceNo(), completed.getType(), variantIds, warehouseIds));
        return response;
    }

    private List<Long> movementWarehouseIds(StockMovement movement) {
        Set<Long> warehouseIds = new TreeSet<>();
        warehouseIds.add(movement.getWarehouseId());
        if (movement.getType() == MovementType.TRANSFER) {
            if (movement.getDestWarehouseId() == null
                    || movement.getDestWarehouseId().equals(movement.getWarehouseId())) {
                throw new BadRequestException("Transfer requires a different destination warehouse");
            }
            warehouseIds.add(movement.getDestWarehouseId());
        }
        return List.copyOf(warehouseIds);
    }

    private Set<StockCacheKey> affectedCacheKeys(StockMovement movement, List<Long> variantIds) {
        Set<StockCacheKey> keys = new LinkedHashSet<>();
        for (Long variantId : variantIds) {
            keys.add(new StockCacheKey(movement.getWarehouseId(), variantId));
            if (movement.getType() == MovementType.TRANSFER) {
                keys.add(new StockCacheKey(movement.getDestWarehouseId(), variantId));
            }
        }
        return keys;
    }

    private void validateOutboundStock(StockMovement movement,
                                       Map<Long, Integer> quantitiesByVariant,
                                       Map<StockKey, StockLevel> levelsByKey) {
        if (movement.getType() != MovementType.EXPORT && movement.getType() != MovementType.TRANSFER) {
            return;
        }
        for (Map.Entry<Long, Integer> entry : quantitiesByVariant.entrySet()) {
            StockLevel source = levelsByKey.get(new StockKey(movement.getWarehouseId(), entry.getKey()));
            int available = source == null ? 0 : source.getQuantity();
            if (available < entry.getValue()) {
                throw new BadRequestException(
                        "Insufficient stock for variant id %d in warehouse id %d"
                                .formatted(entry.getKey(), movement.getWarehouseId()));
            }
        }
    }

    private void applyStockChanges(StockMovement movement,
                                   Map<Long, Integer> quantitiesByVariant,
                                   Map<StockKey, StockLevel> levelsByKey) {
        for (Map.Entry<Long, Integer> entry : quantitiesByVariant.entrySet()) {
            Long variantId = entry.getKey();
            Integer quantity = entry.getValue();
            switch (movement.getType()) {
                case IMPORT -> adjustLevel(levelsByKey, movement.getWarehouseId(), variantId, quantity);
                case EXPORT -> adjustLevel(levelsByKey, movement.getWarehouseId(), variantId, -quantity);
                case TRANSFER -> {
                    adjustLevel(levelsByKey, movement.getWarehouseId(), variantId, -quantity);
                    adjustLevel(levelsByKey, movement.getDestWarehouseId(), variantId, quantity);
                }
                case ADJUSTMENT -> throw new BadRequestException(
                        "ADJUSTMENT movement completion is not supported");
            }
        }
    }

    private void adjustLevel(Map<StockKey, StockLevel> levelsByKey, Long warehouseId, Long variantId, int quantityDelta) {
        StockKey key = new StockKey(warehouseId, variantId);
        StockLevel level = levelsByKey.computeIfAbsent(key, ignored -> StockLevel.builder()
                .warehouseId(warehouseId)
                .variantId(variantId)
                .quantity(0)
                .reservedQuantity(0)
                .build());
        level.setQuantity(level.getQuantity() + quantityDelta);
    }

    private record StockKey(Long warehouseId, Long variantId) {}

    /** Shared creation pipeline for import/export/transfer. Validates, persists movement + items. */
    private MovementResponse createMovement(MovementType type, Long warehouseId, Long destWarehouseId,
                                            String notes, List<CreateMovementItemRequest> items) {
        validateItems(items);

        Warehouse warehouse = loadWarehouse(warehouseId);
        Warehouse destWarehouse = null;
        if (type == MovementType.TRANSFER) {
            if (destWarehouseId == null) {
                throw new BadRequestException("Transfer requires a destination warehouse");
            }
            if (destWarehouseId.equals(warehouseId)) {
                throw new BadRequestException("Transfer source and destination warehouses must be different");
            }
            destWarehouse = loadWarehouse(destWarehouseId);
        }

        Map<Long, ProductVariant> variantsById = loadVariantsByIds(items.stream()
                .map(CreateMovementItemRequest::variantId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        Long createdBy = resolveCurrentUserId();
        String referenceNo = generateReferenceNo(type);

        StockMovement movement = StockMovement.builder()
                .referenceNo(referenceNo)
                .type(type)
                .status(MovementStatus.PENDING_APPROVAL)
                .warehouseId(warehouseId)
                .destWarehouseId(type == MovementType.TRANSFER ? destWarehouseId : null)
                .notes(notes)
                .createdBy(createdBy)
                .build();
        StockMovement saved = stockMovementRepository.save(movement);

        List<StockMovementItem> itemEntities = items.stream()
                .map(item -> StockMovementItem.builder()
                        .movementId(saved.getId())
                        .variantId(item.variantId())
                        .quantity(item.quantity())
                        .unitCost(item.unitCost())
                        .batchNumber(item.batchNumber())
                        .expiryDate(item.expiryDate())
                        .notes(item.notes())
                        .build())
                .toList();
        List<StockMovementItem> savedItems = stockMovementItemRepository.saveAll(itemEntities);

        log.info("Created {} movement {} (id: {}) with {} item(s)", type, referenceNo, saved.getId(), savedItems.size());
        return stockMovementMapper.toResponse(saved, warehouse, destWarehouse, toItemResponses(savedItems, variantsById));
    }

    private void validateItems(List<CreateMovementItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("At least one item is required");
        }
        Set<Long> variantIds = new LinkedHashSet<>();
        for (CreateMovementItemRequest item : items) {
            if (item.variantId() == null) {
                throw new BadRequestException("Variant ID is required for every item");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new BadRequestException("Item quantity must be positive");
            }
            if (!variantIds.add(item.variantId())) {
                throw new BadRequestException("Duplicate variant line for variant id: " + item.variantId());
            }
        }
    }

    private Warehouse loadWarehouse(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", warehouseId));
    }

    private Map<Long, ProductVariant> loadVariantsByIds(Set<Long> variantIds) {
        Map<Long, ProductVariant> variantsById = variantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        for (Long variantId : variantIds) {
            if (!variantsById.containsKey(variantId)) {
                throw new ResourceNotFoundException("ProductVariant", variantId);
            }
        }
        return variantsById;
    }

    private List<MovementItemResponse> toItemResponses(List<StockMovementItem> items, Map<Long, ProductVariant> variantsById) {
        return items.stream()
                .map(item -> stockMovementMapper.toItemResponse(item, variantsById.get(item.getVariantId())))
                .toList();
    }

    private MovementResponse toResponse(StockMovement movement) {
        Warehouse warehouse = loadWarehouse(movement.getWarehouseId());
        Warehouse destWarehouse = movement.getDestWarehouseId() == null ? null : loadWarehouse(movement.getDestWarehouseId());
        List<StockMovementItem> items = stockMovementItemRepository.findByMovementId(movement.getId());
        Map<Long, ProductVariant> variantsById = loadVariantsByIds(items.stream()
                .map(StockMovementItem::getVariantId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        return stockMovementMapper.toResponse(movement, warehouse, destWarehouse, toItemResponses(items, variantsById));
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BadRequestException("No authenticated user is available to record as createdBy");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username))
                .getId();
    }

    private String generateReferenceNo(MovementType type) {
        String prefix = switch (type) {
            case IMPORT -> "IMP";
            case EXPORT -> "EXP";
            case TRANSFER -> "TRF";
            case ADJUSTMENT -> "ADJ";
        };
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            String candidate = "%s-%s-%s".formatted(
                    prefix, datePart, String.format("%04X", REFERENCE_RANDOM.nextInt(0x10000)));
            if (!stockMovementRepository.existsByReferenceNo(candidate)) {
                return candidate;
            }
        }
        throw new DuplicateResourceException("StockMovement", "referenceNo", prefix + "-" + datePart);
    }
}
