import { ProductResponse } from './product.model';

export interface WishlistItemResponse {
  id: number;
  product: ProductResponse;
  createdAt?: string;
}

export interface WishlistStatusResponse {
  productId: number;
  wishlisted: boolean;
}
