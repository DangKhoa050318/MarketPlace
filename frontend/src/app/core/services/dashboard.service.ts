import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';

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
}
