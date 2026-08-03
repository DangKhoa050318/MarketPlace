import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import {
  CollectionResponse,
  CreateCollectionRequest,
  PublishStatus,
  ReorderCollectionItemsRequest,
  UpdateCollectionRequest
} from '../models/collection.model';

@Injectable({ providedIn: 'root' })
export class CollectionService {
  private apiUrl = `${environment.apiUrl}/admin/collections`;

  constructor(private http: HttpClient) {}

  list(opts: { status?: PublishStatus; q?: string; page?: number; size?: number } = {})
    : Observable<ApiResponse<PageResponse<CollectionResponse>>> {
    let params = new HttpParams()
      .set('page', (opts.page ?? 0).toString())
      .set('size', (opts.size ?? 10).toString());
    if (opts.status) params = params.set('status', opts.status);
    if (opts.q) params = params.set('q', opts.q);
    return this.http.get<ApiResponse<PageResponse<CollectionResponse>>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<ApiResponse<CollectionResponse>> {
    return this.http.get<ApiResponse<CollectionResponse>>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateCollectionRequest): Observable<ApiResponse<CollectionResponse>> {
    return this.http.post<ApiResponse<CollectionResponse>>(this.apiUrl, request);
  }

  update(id: number, request: UpdateCollectionRequest): Observable<ApiResponse<CollectionResponse>> {
    return this.http.put<ApiResponse<CollectionResponse>>(`${this.apiUrl}/${id}`, request);
  }

  publish(id: number): Observable<ApiResponse<CollectionResponse>> {
    return this.http.post<ApiResponse<CollectionResponse>>(`${this.apiUrl}/${id}/publish`, {});
  }

  unpublish(id: number): Observable<ApiResponse<CollectionResponse>> {
    return this.http.post<ApiResponse<CollectionResponse>>(`${this.apiUrl}/${id}/unpublish`, {});
  }

  addItem(id: number, productId: number): Observable<ApiResponse<CollectionResponse>> {
    return this.http.post<ApiResponse<CollectionResponse>>(`${this.apiUrl}/${id}/items`, { productId });
  }

  removeItem(id: number, productId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/items/${productId}`);
  }

  reorder(id: number, request: ReorderCollectionItemsRequest): Observable<ApiResponse<CollectionResponse>> {
    return this.http.put<ApiResponse<CollectionResponse>>(`${this.apiUrl}/${id}/items/order`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
