import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PageResponse } from '../models/page-response.model';
import {
  ProductRatingSummary,
  ProductReview,
  RatingSummary,
  ReviewEligibility,
  ReviewPayload
} from '../models/review.model';

export type ReviewSort = 'newest' | 'helpful';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getReviews(productId: number, page = 0, size = 5, rating?: number, sort: ReviewSort = 'newest'):
    Observable<ApiResponse<PageResponse<ProductReview>>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort === 'helpful' ? 'helpfulCount,desc' : 'createdAt,desc');
    if (rating) {
      params = params.set('rating', rating);
    }
    return this.http.get<ApiResponse<PageResponse<ProductReview>>>(
      `${this.apiUrl}/products/${productId}/reviews`,
      { params }
    );
  }

  getSummary(productId: number): Observable<ApiResponse<RatingSummary>> {
    return this.http.get<ApiResponse<RatingSummary>>(
      `${this.apiUrl}/products/${productId}/reviews/summary`
    );
  }

  getProductRatings(ids: number[]): Observable<ApiResponse<ProductRatingSummary[]>> {
    const params = new HttpParams().set('ids', ids.join(','));
    return this.http.get<ApiResponse<ProductRatingSummary[]>>(
      `${this.apiUrl}/products/ratings`, { params }
    );
  }

  getEligibility(productId: number): Observable<ApiResponse<ReviewEligibility>> {
    return this.http.get<ApiResponse<ReviewEligibility>>(
      `${this.apiUrl}/products/${productId}/reviews/eligibility`
    );
  }

  getAdminReviews(page = 0, size = 10, rating?: number, status?: string, productId?: number):
    Observable<ApiResponse<PageResponse<ProductReview>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (rating) {
      params = params.set('rating', rating);
    }
    if (status) {
      params = params.set('status', status);
    }
    if (productId) {
      params = params.set('productId', productId);
    }
    return this.http.get<ApiResponse<PageResponse<ProductReview>>>(`${this.apiUrl}/admin/reviews`, { params });
  }

  adminUpdateStatus(reviewId: number, status: string): Observable<ApiResponse<ProductReview>> {
    return this.http.put<ApiResponse<ProductReview>>(`${this.apiUrl}/admin/reviews/${reviewId}/status`, { status });
  }

  adminReply(reviewId: number, reply: string): Observable<ApiResponse<ProductReview>> {
    return this.http.put<ApiResponse<ProductReview>>(`${this.apiUrl}/admin/reviews/${reviewId}/reply`, { reply });
  }

  create(productId: number, payload: ReviewPayload): Observable<ApiResponse<ProductReview>> {
    return this.http.post<ApiResponse<ProductReview>>(
      `${this.apiUrl}/products/${productId}/reviews`,
      payload
    );
  }

  update(reviewId: number, payload: ReviewPayload): Observable<ApiResponse<ProductReview>> {
    return this.http.put<ApiResponse<ProductReview>>(`${this.apiUrl}/reviews/${reviewId}`, payload);
  }

  getMyReviewedProductIds(): Observable<ApiResponse<number[]>> {
    return this.http.get<ApiResponse<number[]>>(`${this.apiUrl}/reviews/my-reviewed-product-ids`);
  }

  getMyReviewedOrderItemIds(): Observable<ApiResponse<number[]>> {
    return this.http.get<ApiResponse<number[]>>(`${this.apiUrl}/reviews/my-reviewed-order-item-ids`);
  }

  uploadImage(file: File): Observable<ApiResponse<{ url: string }>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<{ url: string }>>(`${this.apiUrl}/uploads/image`, formData);
  }
}
