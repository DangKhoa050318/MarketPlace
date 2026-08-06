export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
export type PaymentMethod = 'COD' | 'CREDIT_CARD' | 'PAYGATE_BNPL' | 'BANK_TRANSFER';
export type PaymentStatus = 'UNPAID' | 'PENDING_PAYGATE' | 'PAID' | 'REFUND_PENDING' | 'PARTIALLY_REFUNDED' | 'REFUNDED';

export interface PaygatePayload {
  orderId: number;
  customerId: number;
  merchantId: string;
  totalAmount: number;
  upfrontAmount: number;
  financeAmount: number;
  paymentChannel: string;
  paymentUrl: string;
  bankAccount?: {
    bankName?: string;
    accountNumber?: string;
    accountHolder?: string;
    amount?: number;
  };
  transferContent?: string;
  qrPayload?: string;
}

export interface OrderItem {
  id: number;
  variantId: number;
  productId?: number;
  sku?: string;
  productName: string;
  variantName?: string;
  unitPrice: number;
  quantity: number;
  refundedQuantity?: number;
  subtotal: number;
}

export interface Order {
  id: number;
  userId: number;
  username?: string;
  userEmail: string;
  shippingAddress?: string;
  totalAmount: number;
  discountAmount?: number;
  shippingFee?: number;
  couponCode?: string;
  status: OrderStatus;
  paymentMethod?: PaymentMethod;
  paymentStatus?: PaymentStatus;
  upfrontAmount?: number;
  financeAmount?: number;
  paygateTransactionRef?: string;
  paygatePayload?: PaygatePayload;
  warehouseId?: number;
  note?: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrderRequest {
  shippingAddress: string;
  note?: string;
  couponCode?: string;
  paymentMethod?: PaymentMethod;
  upfrontAmount?: number;
  financeAmount?: number;
  bnplMonths?: number;
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus;
  note?: string;
}

export interface ReturnRequest {
  id: number;
  orderId: number;
  userId: number;
  orderItemId?: number;
  quantity?: number;
  status: 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'RETURN_RECEIVED' | 'QC_PASSED' | 'QC_FAILED' | 'COMPLETED';
  reason?: string;
  evidenceImageUrls?: string[];
  adminNote?: string;
  qcNote?: string;
  refundWithoutReturn?: boolean;
  createdAt: string;
  updatedAt: string;
}
