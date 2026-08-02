import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api-response.model';

export interface DashboardStats {
  totalOrders: number;
  totalRevenue: number;
  pendingOrders: number;
  completedOrders: number;
  totalProducts: number;
  totalCustomers: number;
}

export interface FunnelStep {
  step: string;
  count: number;
  conversionRate: number;
  dropOffRate: number;
}

export interface FunnelSummary {
  from: string;
  to: string;
  categoryId?: number;
  productId?: number;
  campaign?: string;
  deviceType?: string;
  steps: FunnelStep[];
}

export interface AnalyticsOverview {
  productViews: number;
  addToCarts: number;
  beginCheckouts: number;
  orders: number;
  addToCartRate: number;
  checkoutRate: number;
  orderConversionRate: number;
  returningCustomerRate: number;
  lastUpdatedAt: string;
}

export interface ProductPerformance {
  productId: number;
  productName: string;
  categoryId: number;
  productViews: number;
  wishlists: number;
  addToCarts: number;
  orders: number;
  averageRating: number;
  questionCount: number;
  lastUpdatedAt: string | null;
}

export interface PromotionPerformance {
  campaign: string | null;
  placement: string | null;
  strategy: string | null;
  impressions: number;
  clicks: number;
  clickThroughRate: number;
  addToCarts: number;
  attributedOrders: number;
  lastUpdatedAt: string | null;
}

export interface PromotionTrendPoint {
  date: string;
  impressions: number;
  clicks: number;
  addToCarts: number;
  attributedOrders: number;
}

export type AnalyticsExportType = 'OVERVIEW' | 'PRODUCT_PERFORMANCE' | 'PROMOTION_RECOMMENDATION';
export type AnalyticsExportStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface AnalyticsExportJob {
  id: string;
  type: AnalyticsExportType;
  status: AnalyticsExportStatus;
  fileName: string | null;
  downloadUrl: string | null;
  errorMessage: string | null;
  completedAt: string | null;
  expiresAt: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = `${environment.apiUrl}/admin/dashboard`;

  constructor(private http: HttpClient) {}

  getDashboardStats(): Observable<ApiResponse<DashboardStats>> {
    return this.http.get<ApiResponse<DashboardStats>>(`${this.apiUrl}/stats`);
  }

  getFunnelSummary(from: Date, to: Date): Observable<ApiResponse<FunnelSummary>> {
    return this.http.get<ApiResponse<FunnelSummary>>(`${environment.apiUrl}/admin/analytics/funnel`, {
      params: {
        from: from.toISOString(),
        to: to.toISOString()
      }
    });
  }

  getAnalyticsOverview(from: Date, to: Date): Observable<ApiResponse<AnalyticsOverview>> {
    return this.http.get<ApiResponse<AnalyticsOverview>>(`${environment.apiUrl}/admin/analytics/overview`, {
      params: {
        from: from.toISOString(),
        to: to.toISOString()
      }
    });
  }

  getProductPerformance(
    from: Date,
    to: Date,
    page: number,
    size: number,
    search: string,
    sortBy: string,
    sortDirection: 'asc' | 'desc'
  ): Observable<ApiResponse<PageResponse<ProductPerformance>>> {
    return this.http.get<ApiResponse<PageResponse<ProductPerformance>>>(
      `${environment.apiUrl}/admin/analytics/products/performance`,
      { params: { from: from.toISOString(), to: to.toISOString(), page, size, search, sortBy, sortDirection } }
    );
  }

  getPromotionPerformance(from: Date, to: Date): Observable<ApiResponse<PromotionPerformance[]>> {
    return this.http.get<ApiResponse<PromotionPerformance[]>>(
      `${environment.apiUrl}/admin/analytics/promotion-recommendation/performance`,
      { params: { from: from.toISOString(), to: to.toISOString() } }
    );
  }

  getPromotionTrend(from: Date, to: Date): Observable<ApiResponse<PromotionTrendPoint[]>> {
    return this.http.get<ApiResponse<PromotionTrendPoint[]>>(
      `${environment.apiUrl}/admin/analytics/promotion-recommendation/trend`,
      { params: { from: from.toISOString(), to: to.toISOString() } }
    );
  }

  createExport(type: AnalyticsExportType, from: Date, to: Date): Observable<ApiResponse<AnalyticsExportJob>> {
    return this.http.post<ApiResponse<AnalyticsExportJob>>(`${environment.apiUrl}/admin/analytics/exports`, {
      type,
      from: from.toISOString(),
      to: to.toISOString()
    });
  }

  getExport(id: string): Observable<ApiResponse<AnalyticsExportJob>> {
    return this.http.get<ApiResponse<AnalyticsExportJob>>(`${environment.apiUrl}/admin/analytics/exports/${id}`);
  }

  downloadExport(id: string): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/admin/analytics/exports/${id}/download`, {
      responseType: 'blob'
    });
  }
}
