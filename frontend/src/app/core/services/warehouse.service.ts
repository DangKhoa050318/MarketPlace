import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PageResponse } from '../models/page-response.model';
import { Warehouse, CreateWarehouseRequest, UpdateWarehouseRequest } from '../models/warehouse.model';

@Injectable({ providedIn: 'root' })
export class WarehouseService {
  private apiUrl = `${environment.apiUrl}/warehouses`;

  constructor(private http: HttpClient) {}

  getWarehouses(): Observable<ApiResponse<PageResponse<Warehouse> | Warehouse[]>> {
    return this.http.get<ApiResponse<PageResponse<Warehouse> | Warehouse[]>>(this.apiUrl);
  }

  getWarehouseById(id: number): Observable<ApiResponse<Warehouse>> {
    return this.http.get<ApiResponse<Warehouse>>(`${this.apiUrl}/${id}`);
  }

  createWarehouse(request: CreateWarehouseRequest): Observable<ApiResponse<Warehouse>> {
    return this.http.post<ApiResponse<Warehouse>>(this.apiUrl, request);
  }

  updateWarehouse(id: number, request: UpdateWarehouseRequest): Observable<ApiResponse<Warehouse>> {
    return this.http.put<ApiResponse<Warehouse>>(`${this.apiUrl}/${id}`, request);
  }

  deleteWarehouse(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
