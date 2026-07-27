import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PageResponse } from '../models/page-response.model';
import {
  StockLevel,
  StockSummary,
  StockMovement,
  CreateImportRequest,
  CreateExportRequest,
  CreateTransferRequest
} from '../models/stock.model';

@Injectable({ providedIn: 'root' })
export class StockService {
  private stockUrl = `${environment.apiUrl}/stock`;
  private movementUrl = `${environment.apiUrl}/movements`;

  constructor(private http: HttpClient) {}

  // Stock Level & Summary APIs
  getStockLevels(warehouseId?: number, variantId?: number, page = 0, size = 20): Observable<ApiResponse<PageResponse<StockLevel>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (warehouseId) {
      params = params.set('warehouseId', warehouseId.toString());
    }
    if (variantId) {
      params = params.set('variantId', variantId.toString());
    }

    return this.http.get<ApiResponse<PageResponse<StockLevel>>>(this.stockUrl, { params });
  }

  getLowStock(warehouseId?: number, page = 0, size = 20): Observable<ApiResponse<PageResponse<StockLevel>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (warehouseId) {
      params = params.set('warehouseId', warehouseId.toString());
    }

    return this.http.get<ApiResponse<PageResponse<StockLevel>>>(`${this.stockUrl}/low`, { params });
  }

  getStockSummary(page = 0, size = 20): Observable<ApiResponse<PageResponse<StockSummary>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<PageResponse<StockSummary>>>(`${this.stockUrl}/summary`, { params });
  }

  // Stock Movement APIs
  getMovements(warehouseId?: number, type?: string, status?: string, page = 0, size = 20): Observable<ApiResponse<PageResponse<StockMovement>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (warehouseId) {
      params = params.set('warehouseId', warehouseId.toString());
    }
    if (type) {
      params = params.set('type', type);
    }
    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<ApiResponse<PageResponse<StockMovement>>>(this.movementUrl, { params });
  }

  getMovementById(id: number): Observable<ApiResponse<StockMovement>> {
    return this.http.get<ApiResponse<StockMovement>>(`${this.movementUrl}/${id}`);
  }

  createImport(request: CreateImportRequest): Observable<ApiResponse<StockMovement>> {
    return this.http.post<ApiResponse<StockMovement>>(`${this.movementUrl}/import`, request);
  }

  createExport(request: CreateExportRequest): Observable<ApiResponse<StockMovement>> {
    return this.http.post<ApiResponse<StockMovement>>(`${this.movementUrl}/export`, request);
  }

  createTransfer(request: CreateTransferRequest): Observable<ApiResponse<StockMovement>> {
    return this.http.post<ApiResponse<StockMovement>>(`${this.movementUrl}/transfer`, request);
  }

  completeMovement(id: number): Observable<ApiResponse<StockMovement>> {
    return this.http.post<ApiResponse<StockMovement>>(`${this.movementUrl}/${id}/complete`, {});
  }
}
