import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, EMPTY, Observable, tap } from 'rxjs';
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
  private appliedCouponSubject = new BehaviorSubject<CouponPreviewResponse | null>(null);

  readonly appliedCoupon$ = this.appliedCouponSubject.asObservable();

  constructor(private http: HttpClient) {}

  /** Customer: preview a coupon against the current cart (backend reads the cart itself). */
  preview(code: string): Observable<ApiResponse<CouponPreviewResponse>> {
    return this.http.post<ApiResponse<CouponPreviewResponse>>(`${this.apiUrl}/preview`, { code });
  }

  /** Validate and select a coupon for the current cart without consuming a usage slot. */
  apply(code: string): Observable<ApiResponse<CouponPreviewResponse>> {
    return this.preview(code.trim().toUpperCase()).pipe(
      tap(response => {
        const preview = response.data;
        if (response.success && preview?.valid) {
          this.appliedCouponSubject.next(preview);
        } else {
          this.clearApplied();
        }
      })
    );
  }

  /** Re-check the selected coupon after cart contents change. Transient HTTP errors keep the state. */
  revalidateApplied(): Observable<ApiResponse<CouponPreviewResponse>> {
    const applied = this.appliedCouponSubject.value;
    if (!applied) return EMPTY;
    return this.apply(applied.code);
  }

  clearApplied(): void {
    this.appliedCouponSubject.next(null);
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
