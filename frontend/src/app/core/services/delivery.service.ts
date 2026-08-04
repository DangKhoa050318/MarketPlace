import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  AddDeliveryEventRequest,
  CreateDeliveryRequest,
  Delivery,
  UpdateDeliveryRequest,
  UpdateDeliveryStatusRequest
} from '../models/delivery.model';

@Injectable({ providedIn: 'root' })
export class DeliveryService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getCustomerDelivery(orderId: number): Observable<ApiResponse<Delivery>> {
    return this.http.get<ApiResponse<Delivery>>(`${this.apiUrl}/orders/${orderId}/delivery`);
  }

  getAdminDelivery(orderId: number): Observable<ApiResponse<Delivery>> {
    return this.http.get<ApiResponse<Delivery>>(`${this.apiUrl}/admin/orders/${orderId}/delivery`);
  }

  create(orderId: number, request: CreateDeliveryRequest): Observable<ApiResponse<Delivery>> {
    return this.http.post<ApiResponse<Delivery>>(
      `${this.apiUrl}/admin/orders/${orderId}/delivery`, request);
  }

  update(deliveryId: number, request: UpdateDeliveryRequest): Observable<ApiResponse<Delivery>> {
    return this.http.patch<ApiResponse<Delivery>>(
      `${this.apiUrl}/admin/deliveries/${deliveryId}`, request);
  }

  updateStatus(
    deliveryId: number,
    request: UpdateDeliveryStatusRequest
  ): Observable<ApiResponse<Delivery>> {
    return this.http.patch<ApiResponse<Delivery>>(
      `${this.apiUrl}/admin/deliveries/${deliveryId}/status`, request);
  }

  addEvent(deliveryId: number, request: AddDeliveryEventRequest): Observable<ApiResponse<Delivery>> {
    return this.http.post<ApiResponse<Delivery>>(
      `${this.apiUrl}/admin/deliveries/${deliveryId}/events`, request);
  }
}
