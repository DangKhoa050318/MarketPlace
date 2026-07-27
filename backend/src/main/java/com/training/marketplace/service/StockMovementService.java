package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateExportRequest;
import com.training.marketplace.dto.request.CreateImportRequest;
import com.training.marketplace.dto.request.CreateTransferRequest;
import com.training.marketplace.dto.response.MovementResponse;
import com.training.marketplace.enums.MovementStatus;
import com.training.marketplace.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Creation, inspection, and lifecycle operations for stock movements.
 */
public interface StockMovementService {

    MovementResponse createImport(CreateImportRequest request);

    MovementResponse createExport(CreateExportRequest request);

    MovementResponse createTransfer(CreateTransferRequest request);

    MovementResponse getById(Long id);

    Page<MovementResponse> getAll(
            MovementType type,
            MovementStatus status,
            Long warehouseId,
            Pageable pageable);

    MovementResponse approve(Long id);

    MovementResponse completeMovement(Long id);
}
