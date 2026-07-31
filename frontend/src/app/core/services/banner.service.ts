import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import {
  BannerResponse,
  BannerStatus,
  CreateBannerRequest,
  UpdateBannerRequest
} from '../models/banner.model';

@Injectable({ providedIn: 'root' })
export class BannerService {
  private apiUrl = `${environment.apiUrl}/admin/banners`;

  constructor(private http: HttpClient) {}

  list(opts: { status?: BannerStatus; position?: string; page?: number; size?: number } = {})
    : Observable<ApiResponse<PageResponse<BannerResponse>>> {
    let params = new HttpParams()
      .set('page', (opts.page ?? 0).toString())
      .set('size', (opts.size ?? 10).toString());
    if (opts.status) params = params.set('status', opts.status);
    if (opts.position) params = params.set('position', opts.position);
    return this.http.get<ApiResponse<PageResponse<BannerResponse>>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<ApiResponse<BannerResponse>> {
    return this.http.get<ApiResponse<BannerResponse>>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateBannerRequest): Observable<ApiResponse<BannerResponse>> {
    return this.http.post<ApiResponse<BannerResponse>>(this.apiUrl, request);
  }

  update(id: number, request: UpdateBannerRequest): Observable<ApiResponse<BannerResponse>> {
    return this.http.put<ApiResponse<BannerResponse>>(`${this.apiUrl}/${id}`, request);
  }

  publish(id: number): Observable<ApiResponse<BannerResponse>> {
    return this.http.post<ApiResponse<BannerResponse>>(`${this.apiUrl}/${id}/publish`, {});
  }

  unpublish(id: number): Observable<ApiResponse<BannerResponse>> {
    return this.http.post<ApiResponse<BannerResponse>>(`${this.apiUrl}/${id}/unpublish`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
