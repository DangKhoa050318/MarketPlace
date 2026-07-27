export interface CartItem {
  productId: number;
  productName: string;
  productSlug: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  imageUrl?: string;
}

export interface Cart {
  userId: number;
  items: CartItem[];
  totalAmount: number;
  totalItems: number;
}

export interface AddToCartRequest {
  productId: number;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}
