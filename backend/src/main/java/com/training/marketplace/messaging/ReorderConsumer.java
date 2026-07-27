package com.training.marketplace.messaging;

import com.training.marketplace.config.StockRabbitTopology;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.messaging.event.StockLowEvent;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.repository.ReorderSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReorderConsumer {

    private final ProductVariantRepository variantRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;

    @RabbitListener(queues = StockRabbitTopology.REORDER_SUGGESTION_QUEUE)
    @Transactional
    public void handleStockLow(StockLowEvent event) {
        ProductVariant variant = variantRepository.findById(event.variantId())
                .orElseThrow(() -> new IllegalStateException(
                        "Variant %d from low-stock event %s no longer exists"
                                .formatted(event.variantId(), event.eventId())));

        int inserted = reorderSuggestionRepository.insertPendingIfAbsent(
                event.variantId(),
                event.warehouseId(),
                variant.getReorderQuantity(),
                event.currentQuantity(),
                event.reorderPoint());

        if (inserted == 0) {
            log.debug("Pending reorder suggestion already exists for variant {} in warehouse {}",
                    event.variantId(), event.warehouseId());
            return;
        }

        log.info("Created reorder suggestion for variant {} in warehouse {} from event {}",
                event.variantId(), event.warehouseId(), event.eventId());
    }
}
