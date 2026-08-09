package com.training.marketplace.service;

import com.training.marketplace.dto.response.PaygatePayloadResponse;
import com.training.marketplace.entity.Order;
import com.training.marketplace.enums.PaymentMethod;

public interface PaymentService {

    PaygatePayloadResponse createPaymentSession(Order order, PaymentMethod paymentMethod);

    PaygatePayloadResponse retryOrderPayment(Long userId, Long orderId);

    Order cancelVietQrPayment(Long userId, Long orderId);

    Order confirmVietQrPayment(Long userId, Long orderId);

    void cancelPayment(Order order, String reason);
}
