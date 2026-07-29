export type DiscountType = 'PERCENT' | 'FIXED';
export type PromotionScopeType = 'CART' | 'PRODUCT' | 'CATEGORY';
export type ScopeRefType = 'PRODUCT' | 'CATEGORY';

export interface ScopeRef {
  refType: ScopeRefType;
  refId: number;
}

export interface PromotionCodeResponse {
  id: number;
  code: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscount?: number;
  minOrderAmount: number;
  scopeType: PromotionScopeType;
  scopes: ScopeRef[];
  usageLimit?: number;
  perUserLimit?: number;
  usedCount: number;
  startsAt?: string;
  expiresAt?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreatePromotionCodeRequest {
  code: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscount?: number | null;
  minOrderAmount?: number | null;
  scopeType?: PromotionScopeType;
  scopes?: ScopeRef[] | null;
  usageLimit?: number | null;
  perUserLimit?: number | null;
  startsAt?: string | null;
  expiresAt?: string | null;
  active?: boolean;
}

export type UpdatePromotionCodeRequest = Partial<Omit<CreatePromotionCodeRequest, 'code'>>;

export interface CouponPreviewResponse {
  code: string;
  valid: boolean;
  reason?: string;
  discountType?: DiscountType;
  eligibleSubtotal: number;
  discountAmount: number;
  cartSubtotal: number;
  newTotal: number;
}
