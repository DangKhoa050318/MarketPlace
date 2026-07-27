package com.training.marketplace.messaging;

import com.training.marketplace.config.StockRabbitTopology;
import com.training.marketplace.messaging.event.StockLowEvent;
import com.training.marketplace.messaging.event.StockMovementCompletedEvent;
import com.training.marketplace.repository.StockLevelRepository;
import com.training.marketplace.repository.projection.StockThresholdProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockUpdateConsumer {

    private final StockLevelRepository stockLevelRepository;
    private final StockEventPublisher stockEventPublisher;

    @RabbitListener(queues = StockRabbitTopology.STOCK_UPDATE_QUEUE)
    @Transactional(readOnly = true)
    public void handleMovementCompleted(StockMovementCompletedEvent event) {
        log.debug(
                "Evaluating committed stock for movement event {}",
                event.eventId());

        List<StockThresholdProjection> levels =
                stockLevelRepository.findAffectedWithThresholds(
                        event.warehouseIds(), event.variantIds());
        validateAllAffectedLevelsExist(event, levels);

        for (StockThresholdProjection level : levels) {
            if (level.getQuantity() <= level.getReorderPoint()) {
                stockEventPublisher.publishStockLow(
                        StockLowEvent.create(
                                event,
                                level.getVariantId(),
                                level.getWarehouseId(),
                                level.getQuantity(),
                                level.getReorderPoint()));
            }
        }
    }

    private void validateAllAffectedLevelsExist(
            StockMovementCompletedEvent event,
            List<StockThresholdProjection> levels) {
        Set<StockKey> actual = new HashSet<>();
        for (StockThresholdProjection level : levels) {
            actual.add(new StockKey(
                    level.getWarehouseId(), level.getVariantId()));
        }

        for (Long warehouseId : event.warehouseIds()) {
            for (Long variantId : event.variantIds()) {
                if (!actual.contains(new StockKey(warehouseId, variantId))) {
                    throw new IllegalStateException(
                            "Committed stock level is missing for warehouse id %d and variant id %d"
                                    .formatted(warehouseId, variantId));
                }
            }
        }
    }

    private record StockKey(Long warehouseId, Long variantId) {
    }
}
