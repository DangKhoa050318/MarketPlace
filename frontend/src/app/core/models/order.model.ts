export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
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
  status: OrderStatus;
  note?: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus;
  note?: string;
}
