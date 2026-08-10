export type ChatIntent =
  | 'GREETING'
  | 'THANKS'
  | 'HELP'
  | 'BEST_SELLER'
  | 'PRODUCT_DISCOVERY'
  | 'CAMPAIGN_OFFERS'
  | 'CHECKOUT';

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
  recommendedVariantId: number;
  recommendedVariantName: string;
  recommendedVariantPrice: number;
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
  couponCode?: string;
}

export interface ChatOrderSummary {
  id: number;
  status: string;
  paymentMethod: 'COD' | 'CREDIT_CARD' | 'PAYGATE_BNPL' | 'BANK_TRANSFER' | 'WALLET';
  paymentStatus: string;
  totalAmount: number;
  discountAmount: number;
  shippingFee: number;
  couponCode?: string;
  shippingAddress: string;
  note?: string;
  itemCount: number;
  paymentUrl?: string;
  paymentExpiresAt?: string;
  createdAt: string;
}

export interface ChatMessageResponse {
  conversationId: string;
  messageId: string;
  answer: string;
  intents: ChatIntent[];
  products: ChatProductCard[];
  quickReplies: string[];
  order?: ChatOrderSummary;
  traceId: string;
}
