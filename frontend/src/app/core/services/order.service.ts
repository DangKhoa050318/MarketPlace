import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import { Order, OrderItem, CreateOrderRequest } from '../models/order.model';

export { Order, OrderItem, CreateOrderRequest };

@Injectable({ providedIn: 'root' })
export class OrderService {
  private apiUrl = `${environment.apiUrl}/orders`;

  constructor(private http: HttpClient) {}

  createOrder(request: CreateOrderRequest): Observable<ApiResponse<Order>> {
    return this.http.post<ApiResponse<Order>>(this.apiUrl, request);
  }

  getUserOrders(page = 0, size = 10): Observable<ApiResponse<PageResponse<Order>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<Order>>>(this.apiUrl, { params });
  }

  getUserOrderById(id: number): Observable<ApiResponse<Order>> {
    return this.http.get<ApiResponse<Order>>(`${this.apiUrl}/${id}`);
  }

  cancelOrder(id: number): Observable<ApiResponse<Order>> {
    return this.http.put<ApiResponse<Order>>(`${this.apiUrl}/${id}/cancel`, {});
  }

  confirmPaygatePayment(orderId: string, transactionRef?: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/payments/paygate-webhook`, {
      event: 'PAYMENT_COMPLETED',
      orderId,
      transactionRef: transactionRef || 'TXN-DIRECT-CALLBACK',
      status: 'SUCCESS'
    });
  }

  confirmReceived(id: number): Observable<ApiResponse<Order>> {
    return this.http.put<ApiResponse<Order>>(`${this.apiUrl}/${id}/confirm-received`, {});
  }
}
