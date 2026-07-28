export enum AnalyticsEventType {
  PageView = 'PAGE_VIEW',
  ProductView = 'PRODUCT_VIEW',
  Search = 'SEARCH',
  AddToWishlist = 'ADD_TO_WISHLIST',
  AddToCart = 'ADD_TO_CART',
  BeginCheckout = 'BEGIN_CHECKOUT',
  OrderCreated = 'ORDER_CREATED'
}

export interface TrackAnalyticsEventRequest {
  type: AnalyticsEventType;
  occurredAt: string;
  properties: Record<string, unknown>;
}
