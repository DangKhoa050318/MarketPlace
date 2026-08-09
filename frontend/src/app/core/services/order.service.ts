import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import { Order, OrderItem, CreateOrderRequest, PaygatePayload, ReturnRequest } from '../models/order.model';

export { Order, OrderItem, CreateOrderRequest, PaygatePayload };

export interface CreateReturnRequestPayload {
  orderItemId?: number;
  quantity?: number;
  reason: string;
  evidenceImageUrls?: string[];
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private apiUrl = `${environment.apiUrl}/orders`;

  constructor(private http: HttpClient) {}

  createOrder(request: CreateOrderRequest): Observable<ApiResponse<Order>> {
    return this.http.post<ApiResponse<Order>>(this.apiUrl, request);
  }

  retryPayment(id: number): Observable<ApiResponse<PaygatePayload>> {
    return this.http.post<ApiResponse<PaygatePayload>>(`${this.apiUrl}/${id}/retry-payment`, {});
  }

  getUserOrders(page = 0, size = 10, status?: string, paymentStatus?: string, search?: string): Observable<ApiResponse<PageResponse<Order>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }
    if (paymentStatus) {
      params = params.set('paymentStatus', paymentStatus);
    }
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<ApiResponse<PageResponse<Order>>>(this.apiUrl, { params });
  }

  getUserOrderById(id: number): Observable<ApiResponse<Order>> {
    return this.http.get<ApiResponse<Order>>(`${this.apiUrl}/${id}`);
  }

  cancelOrder(id: number): Observable<ApiResponse<Order>> {
    return this.http.put<ApiResponse<Order>>(`${this.apiUrl}/${id}/cancel`, {});
  }

  createReturnRequest(id: number, request: CreateReturnRequestPayload): Observable<ApiResponse<ReturnRequest>> {
    return this.http.post<ApiResponse<ReturnRequest>>(`${this.apiUrl}/${id}/return-requests`, request);
  }

  confirmVietQrPayment(id: number): Observable<ApiResponse<Order>> {
    return this.http.post<ApiResponse<Order>>(`${this.apiUrl}/${id}/confirm-vietqr`, {});
  }

  cancelVietQrPayment(id: number): Observable<ApiResponse<Order>> {
    return this.http.post<ApiResponse<Order>>(`${this.apiUrl}/${id}/cancel-vietqr`, {});
  }

  confirmReceived(id: number): Observable<ApiResponse<Order>> {
    return this.http.put<ApiResponse<Order>>(`${this.apiUrl}/${id}/confirm-received`, {});
  }
}
