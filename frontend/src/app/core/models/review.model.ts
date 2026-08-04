export interface ProductReview {
  id: number;
  userId: number;
  username: string;
  userFullName?: string;
  productId: number;
  productName?: string;
  rating: number;
  title: string;
  content: string;
  imageUrl?: string;
  variantName?: string;
  sku?: string;
  status: 'APPROVED' | 'HIDDEN' | 'DELETED';
  isVerifiedPurchase: boolean;
  isEdited: boolean;
  helpfulCount: number;
  sellerReply?: string;
  sellerReplyAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RatingSummary {
  productId: number;
  averageRating: number;
  totalReviews: number;
  starCounts: Record<number, number>;
}

export interface ProductRatingSummary {
  productId: number;
  averageRating: number;
  reviewCount: number;
}

export interface ReviewEligibility {
  eligible: boolean;
  isVerifiedPurchase: boolean;
  orderItemId?: number;
  existingReview?: ProductReview;
  message: string;
}

export interface ReviewPayload {
  rating: number;
  title?: string;
  content?: string;
  imageUrl?: string;
  orderItemId?: number;
}
