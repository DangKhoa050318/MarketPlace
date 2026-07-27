import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PageResponse } from '../models/page-response.model';
import { Order, OrderStatus, UpdateOrderStatusRequest } from '../models/order.model';

@Injectable({
  providedIn: 'root'
})
export class AdminOrderService {
  private apiUrl = `${environment.apiUrl}/admin/orders`;

  constructor(private http: HttpClient) {}

  getAdminOrders(
    status?: string,
    page: number = 0,
    size: number = 10,
    sort: string = 'createdAt,desc'
  ): Observable<ApiResponse<PageResponse<Order>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }

    return this.http.get<ApiResponse<PageResponse<Order>>>(this.apiUrl, { params });
  }

  updateOrderStatus(orderId: number, status: OrderStatus, note?: string): Observable<ApiResponse<Order>> {
    const payload: UpdateOrderStatusRequest = { status, note };
    return this.http.put<ApiResponse<Order>>(`${this.apiUrl}/${orderId}/status`, payload);
  }
}
