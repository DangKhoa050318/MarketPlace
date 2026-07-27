package com.training.marketplace.consumer;

import com.training.marketplace.config.RabbitMQConfig;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.event.OrderCreatedEvent;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.service.InventoryFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final OrderRepository orderRepository;
    private final InventoryFacade inventoryFacade;

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void processPayment(OrderCreatedEvent event) {
        log.info("Received Payment Event: eventId={}, orderId={}, userId={}, totalAmount={}",
                event.eventId(), event.orderId(), event.userId(), event.totalAmount());

        try {
            boolean paymentSuccess = simulatePaymentGateway(event);

            Order order = orderRepository.findById(event.orderId()).orElse(null);
            if (order == null) {
                log.warn("Order #{} not found during payment processing", event.orderId());
                return;
            }

            if (paymentSuccess) {
                if (order.getStatus() == OrderStatus.PENDING) {
                    order.setStatus(OrderStatus.CONFIRMED);
                    orderRepository.save(order);
                    log.info("Mock Payment SUCCESSFUL: Order #{} -> CONFIRMED", event.orderId());
                }
            } else {
                handlePaymentFailure(order);
            }
        } catch (Exception e) {
            log.error("Payment processing error for orderId={}: {}", event.orderId(), e.getMessage(), e);
            orderRepository.findById(event.orderId()).ifPresent(this::handlePaymentFailure);
        }
    }

    private boolean simulatePaymentGateway(OrderCreatedEvent event) {
        return true; // mock gateway: succeeds by default
    }

    /** Compensating action: cancel the order and release the stock reservation held for it. */
    private void handlePaymentFailure(Order order) {
        if (order.getStatus() == OrderStatus.PENDING) {
            log.warn("Mock Payment FAILED for Order #{}: cancelling and releasing reservation", order.getId());
            order.setStatus(OrderStatus.CANCELLED);

            if (order.getWarehouseId() != null) {
                Map<Long, Integer> quantityByVariant = new LinkedHashMap<>();
                for (OrderItem item : order.getItems()) {
                    quantityByVariant.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
                }
                inventoryFacade.release(order.getWarehouseId(), quantityByVariant);
            }
            orderRepository.save(order);
        }
    }
}
