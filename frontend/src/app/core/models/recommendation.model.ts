import {
  RecommendationPlacement,
  RecommendationStrategyType
} from './analytics-event.model';
import { ProductResponse } from './product.model';

export interface RecommendationQuery {
  placement: RecommendationPlacement;
  productId?: number;
  categoryId?: number;
  limit?: number;
}

export interface RecommendationItem {
  position: number;
  product: ProductResponse;
  score: number;
  reason?: string;
}

export interface RecommendationResponse {
  requestId: string;
  placement: RecommendationPlacement;
  strategy: RecommendationStrategyType;
  generatedAt: string;
  items: RecommendationItem[];
}
