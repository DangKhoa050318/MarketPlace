import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import {
  CouponPreviewResponse,
  CreatePromotionCodeRequest,
  DiscountType,
  PromotionCodeResponse,
  UpdatePromotionCodeRequest
} from '../models/promotion.model';

@Injectable({ providedIn: 'root' })
export class PromotionService {
  private apiUrl = `${environment.apiUrl}/coupons`;

  constructor(private http: HttpClient) {}

  /** Customer: preview a coupon against the current cart (backend reads the cart itself). */
  preview(code: string): Observable<ApiResponse<CouponPreviewResponse>> {
    return this.http.post<ApiResponse<CouponPreviewResponse>>(`${this.apiUrl}/preview`, { code });
  }

  // --- Admin ---
  list(opts: { active?: boolean; type?: DiscountType; q?: string; page?: number; size?: number } = {})
    : Observable<ApiResponse<PageResponse<PromotionCodeResponse>>> {
    let params = new HttpParams()
      .set('page', (opts.page ?? 0).toString())
      .set('size', (opts.size ?? 10).toString());
    if (opts.active !== undefined && opts.active !== null) params = params.set('active', opts.active);
    if (opts.type) params = params.set('type', opts.type);
    if (opts.q) params = params.set('q', opts.q);
    return this.http.get<ApiResponse<PageResponse<PromotionCodeResponse>>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<ApiResponse<PromotionCodeResponse>> {
    return this.http.get<ApiResponse<PromotionCodeResponse>>(`${this.apiUrl}/${id}`);
  }

  create(request: CreatePromotionCodeRequest): Observable<ApiResponse<PromotionCodeResponse>> {
    return this.http.post<ApiResponse<PromotionCodeResponse>>(this.apiUrl, request);
  }

  update(id: number, request: UpdatePromotionCodeRequest): Observable<ApiResponse<PromotionCodeResponse>> {
    return this.http.put<ApiResponse<PromotionCodeResponse>>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
