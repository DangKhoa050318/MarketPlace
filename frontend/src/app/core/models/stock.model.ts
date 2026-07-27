export interface StockLevel {
  id: number;
  variantId: number;
  sku: string;
  variantName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  minStock?: number;
  maxStock?: number;
  reorderPoint?: number;
  version?: number;
  updatedAt?: string;
}

export interface StockSummary {
  variantId: number;
  sku: string;
  variantName: string;
  totalQuantity: number;
  totalReserved: number;
  totalAvailable: number;
  warehouseCount: number;
}

export type MovementType = 'IMPORT' | 'EXPORT' | 'TRANSFER' | 'ADJUSTMENT';
export type MovementStatus = 'DRAFT' | 'APPROVED' | 'COMPLETED' | 'CANCELLED';

export interface MovementItem {
  id?: number;
  variantId: number;
  sku?: string;
  variantName?: string;
  quantity: number;
  notes?: string;
}

export interface StockMovement {
  id: number;
  referenceNo: string;
  type: MovementType;
  status: MovementStatus;
  warehouseId: number;
  warehouseCode?: string;
  warehouseName?: string;
  destWarehouseId?: number;
  destWarehouseCode?: string;
  destWarehouseName?: string;
  notes?: string;
  createdBy?: number;
  approvedBy?: number;
  items: MovementItem[];
  createdAt: string;
}

export interface CreateMovementItemRequest {
  variantId: number;
  quantity: number;
  notes?: string;
}

export interface CreateImportRequest {
  warehouseId: number;
  notes?: string;
  items: CreateMovementItemRequest[];
}

export interface CreateExportRequest {
  warehouseId: number;
  notes?: string;
  items: CreateMovementItemRequest[];
}

export interface CreateTransferRequest {
  sourceWarehouseId: number;
  destWarehouseId: number;
  notes?: string;
  items: CreateMovementItemRequest[];
}

export interface ReorderSuggestion {
  id: number;
  variantId: number;
  sku: string;
  variantName: string;
  warehouseId: number;
  warehouseName: string;
  currentAvailable: number;
  reorderQuantity: number;
  status: string;
  createdAt: string;
}
