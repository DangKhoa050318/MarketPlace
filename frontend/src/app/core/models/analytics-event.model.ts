export enum AnalyticsEventType {
  PageView = 'PAGE_VIEW',
  ProductView = 'PRODUCT_VIEW',
  Search = 'SEARCH',
  AddToWishlist = 'ADD_TO_WISHLIST',
  AddToCart = 'ADD_TO_CART',
  BeginCheckout = 'BEGIN_CHECKOUT',
  OrderCreated = 'ORDER_CREATED',
  RecommendationImpression = 'RECOMMENDATION_IMPRESSION',
  RecommendationClick = 'RECOMMENDATION_CLICK',
  Purchase = 'PURCHASE'
}

export enum AnalyticsEventSource {
  Direct = 'DIRECT',
  Catalog = 'CATALOG',
  Search = 'SEARCH',
  RecentlyViewed = 'RECENTLY_VIEWED',
  Recommendation = 'RECOMMENDATION',
  Cart = 'CART',
  Checkout = 'CHECKOUT',
  OrderService = 'ORDER_SERVICE'
}

export enum RecommendationPlacement {
  ProductDetailSimilar = 'PRODUCT_DETAIL_SIMILAR',
  ProductDetailCoViewed = 'PRODUCT_DETAIL_CO_VIEWED',
  ProductDetailCoPurchased = 'PRODUCT_DETAIL_CO_PURCHASED',
  HomeBestSellers = 'HOME_BEST_SELLERS',
  CategoryBestSellers = 'CATEGORY_BEST_SELLERS'
}

export enum RecommendationStrategyType {
  Similar = 'SIMILAR',
  BestSeller = 'BEST_SELLER',
  CoViewed = 'CO_VIEWED',
  CoPurchased = 'CO_PURCHASED'
}

export interface AnalyticsEventContext {
  productId?: number;
  variantId?: number;
  source?: AnalyticsEventSource;
  placement?: RecommendationPlacement;
  recommendationRequestId?: string;
  strategy?: RecommendationStrategyType;
  position?: number;
  quantity?: number;
  orderId?: number;
  unitPrice?: number;
}

export interface TrackAnalyticsEventRequest {
  eventId: string;
  schemaVersion: 1;
  type: AnalyticsEventType;
  occurredAt: string;
  productId?: number;
  variantId?: number;
  source?: AnalyticsEventSource;
  placement?: RecommendationPlacement;
  recommendationRequestId?: string;
  strategy?: RecommendationStrategyType;
  position?: number;
  quantity?: number;
  orderId?: number;
  unitPrice?: number;
  properties: Record<string, unknown>;
}
