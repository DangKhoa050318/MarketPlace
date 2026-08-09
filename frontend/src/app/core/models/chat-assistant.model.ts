export type ChatIntent =
  | 'GREETING'
  | 'THANKS'
  | 'HELP'
  | 'BEST_SELLER'
  | 'PRODUCT_DISCOVERY'
  | 'CAMPAIGN_OFFERS';

export interface ChatPageContext {
  productId?: number;
  categoryId?: number;
}

export interface ChatVoucher {
  campaignId: number;
  campaignName: string;
  code: string;
  discountType: 'PERCENT' | 'FIXED';
  discountValue: number;
  maxDiscount?: number;
  minOrderAmount: number;
  scopeType: 'CART' | 'PRODUCT' | 'CATEGORY';
  expiresAt?: string;
  discountText: string;
}

export interface ChatProductCard {
  productId: number;
  slug: string;
  name: string;
  imageUrl?: string;
  categoryId: number;
  categoryName: string;
  brand?: string;
  minPrice: number;
  maxPrice: number;
  availableStock: number;
  reason: string;
  voucher?: ChatVoucher;
}

export interface ChatMessageRequest {
  conversationId?: string;
  message: string;
  pageContext?: ChatPageContext;
}

export interface ChatMessageResponse {
  conversationId: string;
  messageId: string;
  answer: string;
  intents: ChatIntent[];
  products: ChatProductCard[];
  quickReplies: string[];
  traceId: string;
}
