package com.training.marketplace.service.impl;

import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.service.InventoryFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryJob {

    private final OrderRepository orderRepository;
    private final InventoryFacade inventoryFacade;

    /**
     * Runs every 1 minute to scan and auto-cancel PENDING orders whose payment session has expired.
     * Releases reserved stock back to stock_levels.
     */
    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void cancelExpiredPendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.findByStatusAndPaygateExpiresAtBefore(OrderStatus.PENDING, now);

        if (expiredOrders == null || expiredOrders.isEmpty()) {
            return;
        }

        log.info("Found {} expired PENDING orders to auto-cancel", expiredOrders.size());

        for (Order order : expiredOrders) {
            cancelExpiredOrder(order);
        }
    }

    public void cancelExpiredOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.UNPAID);

        // Aggregate quantities per variant to release
        Map<Long, Integer> quantityByVariant = new LinkedHashMap<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getVariantId() != null && item.getQuantity() != null) {
                    quantityByVariant.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
                }
            }
        }

        if (order.getWarehouseId() != null && !quantityByVariant.isEmpty()) {
            try {
                inventoryFacade.release(order.getWarehouseId(), quantityByVariant);
                log.info("Released reserved stock for expired order id={}", order.getId());
            } catch (Exception ex) {
                log.error("Failed to release stock for expired order id={}: {}", order.getId(), ex.getMessage(), ex);
            }
        }

        orderRepository.save(order);
        log.info("Order id={} has been automatically CANCELLED due to payment session expiration", order.getId());
    }
}
