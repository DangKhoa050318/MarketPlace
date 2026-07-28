export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItem {
  id: number;
  variantId: number;
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
  couponCode?: string;
  status: OrderStatus;
  warehouseId?: number;
  note?: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus;
  note?: string;
}
