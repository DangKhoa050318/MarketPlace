import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api-response.model';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Order {
  id: number;
  userId: number;
  status: string;
  totalAmount: number;
  shippingAddress: string;
  note?: string;
  items: OrderItem[];
  createdAt: string;
}

export interface CreateOrderRequest {
  shippingAddress: string;
  note?: string;
}

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
}
