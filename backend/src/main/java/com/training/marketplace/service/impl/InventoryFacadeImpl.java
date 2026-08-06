package com.training.marketplace.service.impl;

import com.training.marketplace.entity.StockLevel;
import com.training.marketplace.entity.Warehouse;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.StockLevelRepository;
import com.training.marketplace.repository.WarehouseRepository;
import com.training.marketplace.service.InventoryFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryFacadeImpl implements InventoryFacade {

    private final StockLevelRepository stockLevelRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public Long defaultWarehouseId() {
        return warehouseRepository.findAll().stream()
                .filter(Warehouse::isActive)
                .map(Warehouse::getId)
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new BadRequestException("No active warehouse available for fulfilment"));
    }

    @Override
    @Transactional
    public void reserve(Long warehouseId, Map<Long, Integer> quantityByVariant) {
        Map<Long, StockLevel> rows = lock(warehouseId, quantityByVariant);
        for (Map.Entry<Long, Integer> e : quantityByVariant.entrySet()) {
            StockLevel sl = require(rows, warehouseId, e.getKey());
            int available = sl.getQuantity() - sl.getReservedQuantity();
            if (available < e.getValue()) {
                throw new BadRequestException(String.format(
                        "Insufficient stock for variant %d at warehouse %d (available %d, requested %d)",
                        e.getKey(), warehouseId, available, e.getValue()));
            }
            sl.setReservedQuantity(sl.getReservedQuantity() + e.getValue());
        }
        stockLevelRepository.saveAll(rows.values());
    }

    @Override
    @Transactional
    public void release(Long warehouseId, Map<Long, Integer> quantityByVariant) {
        Map<Long, StockLevel> rows = lock(warehouseId, quantityByVariant);
        for (Map.Entry<Long, Integer> e : quantityByVariant.entrySet()) {
            StockLevel sl = rows.get(e.getKey());
            if (sl != null) {
                sl.setReservedQuantity(Math.max(0, sl.getReservedQuantity() - e.getValue()));
            }
        }
        stockLevelRepository.saveAll(rows.values());
    }

    @Override
    @Transactional
    public void fulfill(Long warehouseId, Map<Long, Integer> quantityByVariant) {
        Map<Long, StockLevel> rows = lock(warehouseId, quantityByVariant);
        for (Map.Entry<Long, Integer> e : quantityByVariant.entrySet()) {
            StockLevel sl = require(rows, warehouseId, e.getKey());
            int qty = e.getValue();
            if (sl.getQuantity() < qty) {
                throw new BadRequestException(String.format(
                        "Cannot fulfil variant %d: on-hand %d < %d", e.getKey(), sl.getQuantity(), qty));
            }
            sl.setQuantity(sl.getQuantity() - qty);
            sl.setReservedQuantity(Math.max(0, sl.getReservedQuantity() - qty));
        }
        stockLevelRepository.saveAll(rows.values());
    }

    @Override
    @Transactional
    public void returnStock(Long warehouseId, Map<Long, Integer> quantityByVariant) {
        Map<Long, StockLevel> rows = lock(warehouseId, quantityByVariant);
        for (Map.Entry<Long, Integer> e : quantityByVariant.entrySet()) {
            StockLevel sl = require(rows, warehouseId, e.getKey());
            // A returned order was already fulfilled, so add back on-hand stock; reservations stay unchanged.
            sl.setQuantity(sl.getQuantity() + e.getValue());
        }
        stockLevelRepository.saveAll(rows.values());
    }

    @Override
    @Transactional
    public void restockReturn(Long warehouseId, Map<Long, Integer> quantityByVariant) {
        Map<Long, StockLevel> rows = lock(warehouseId, quantityByVariant);
        for (Map.Entry<Long, Integer> e : quantityByVariant.entrySet()) {
            StockLevel sl = require(rows, warehouseId, e.getKey());
            sl.setQuantity(sl.getQuantity() + e.getValue());
        }
        stockLevelRepository.saveAll(rows.values());
    }

    /** Locks the stock_levels rows for the given variants (ascending variantId → deadlock-free). */
    private Map<Long, StockLevel> lock(Long warehouseId, Map<Long, Integer> quantityByVariant) {
        List<Long> variantIds = new ArrayList<>(quantityByVariant.keySet());
        variantIds.sort(Comparator.naturalOrder());
        List<StockLevel> locked = stockLevelRepository.findAllForUpdate(List.of(warehouseId), variantIds);
        return locked.stream().collect(Collectors.toMap(StockLevel::getVariantId, x -> x));
    }

    private StockLevel require(Map<Long, StockLevel> rows, Long warehouseId, Long variantId) {
        StockLevel sl = rows.get(variantId);
        if (sl == null) {
            throw new BadRequestException(
                    "No stock record for variant " + variantId + " at warehouse " + warehouseId);
        }
        return sl;
    }
}
