package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateReturnRequest;
import com.training.marketplace.dto.request.AdminReturnDecisionRequest;
import com.training.marketplace.dto.request.PartialRefundRequest;
import com.training.marketplace.dto.request.ReturnQcRequest;
import com.training.marketplace.dto.response.ReturnRequestResponse;

import java.util.List;

public interface ReturnRequestService {
    ReturnRequestResponse create(Long userId, Long orderId, CreateReturnRequest request);

    List<ReturnRequestResponse> getOpenRequests();

    ReturnRequestResponse decide(Long returnRequestId, AdminReturnDecisionRequest request);

    ReturnRequestResponse recordQc(Long returnRequestId, ReturnQcRequest request);

    ReturnRequestResponse partialRefund(Long orderId, PartialRefundRequest request);
}
