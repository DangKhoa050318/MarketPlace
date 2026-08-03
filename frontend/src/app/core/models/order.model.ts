export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
export type PaymentMethod = 'COD' | 'CREDIT_CARD' | 'PAYGATE_BNPL';
export type PaymentStatus = 'UNPAID' | 'PENDING_PAYGATE' | 'PAID' | 'REFUNDED';

export interface PaygatePayload {
  orderId: number;
  customerId: number;
  merchantId: string;
  totalAmount: number;
  upfrontAmount: number;
  financeAmount: number;
  paymentChannel: string;
  paymentUrl: string;
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
