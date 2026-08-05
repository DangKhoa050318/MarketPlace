package com.training.marketplace.service;

import com.training.marketplace.dto.request.AddDeliveryEventRequest;
import com.training.marketplace.dto.request.CreateDeliveryRequest;
import com.training.marketplace.dto.request.UpdateDeliveryRequest;
import com.training.marketplace.dto.request.UpdateDeliveryStatusRequest;
import com.training.marketplace.dto.response.DeliveryResponse;

public interface DeliveryService {

    DeliveryResponse create(Long orderId, Long actorUserId, CreateDeliveryRequest request);

    DeliveryResponse getForCustomer(Long orderId, Long customerUserId);

    DeliveryResponse getForAdmin(Long orderId);

    DeliveryResponse update(Long deliveryId, UpdateDeliveryRequest request);

    DeliveryResponse updateStatus(Long deliveryId, Long actorUserId, UpdateDeliveryStatusRequest request);

    DeliveryResponse addEvent(Long deliveryId, Long actorUserId, AddDeliveryEventRequest request);

    /**
     * Close the delivery when the customer confirms receipt (keeps delivery + order in sync).
     * No-op if the order has no delivery record; throws if the parcel has not been picked up yet
     * (delivery still PENDING); idempotent if already DELIVERED.
     */
    void completeForCustomerConfirmation(Long orderId, Long actorUserId);
}
