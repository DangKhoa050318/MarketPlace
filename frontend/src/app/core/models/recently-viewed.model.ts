import { ProductResponse } from './product.model';

export interface RecentlyViewedProductResponse {
  id: number;
  viewedAt: string;
  product: ProductResponse;
}
