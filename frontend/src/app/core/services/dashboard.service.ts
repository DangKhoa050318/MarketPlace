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
}
