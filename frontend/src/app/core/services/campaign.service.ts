import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import {
  CampaignResponse,
  CampaignStatus,
  CreateCampaignRequest,
  UpdateCampaignRequest
} from '../models/campaign.model';

@Injectable({ providedIn: 'root' })
export class CampaignService {
  private apiUrl = `${environment.apiUrl}/admin/campaigns`;

  constructor(private http: HttpClient) {}

  list(opts: { status?: CampaignStatus; q?: string; page?: number; size?: number } = {})
    : Observable<ApiResponse<PageResponse<CampaignResponse>>> {
    let params = new HttpParams()
      .set('page', (opts.page ?? 0).toString())
      .set('size', (opts.size ?? 10).toString());
    if (opts.status) params = params.set('status', opts.status);
    if (opts.q) params = params.set('q', opts.q);
    return this.http.get<ApiResponse<PageResponse<CampaignResponse>>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<ApiResponse<CampaignResponse>> {
    return this.http.get<ApiResponse<CampaignResponse>>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateCampaignRequest): Observable<ApiResponse<CampaignResponse>> {
    return this.http.post<ApiResponse<CampaignResponse>>(this.apiUrl, request);
  }

  update(id: number, request: UpdateCampaignRequest): Observable<ApiResponse<CampaignResponse>> {
    return this.http.put<ApiResponse<CampaignResponse>>(`${this.apiUrl}/${id}`, request);
  }

  publish(id: number): Observable<ApiResponse<CampaignResponse>> {
    return this.http.post<ApiResponse<CampaignResponse>>(`${this.apiUrl}/${id}/publish`, {});
  }

  archive(id: number): Observable<ApiResponse<CampaignResponse>> {
    return this.http.post<ApiResponse<CampaignResponse>>(`${this.apiUrl}/${id}/archive`, {});
  }

  restore(id: number): Observable<ApiResponse<CampaignResponse>> {
    return this.http.post<ApiResponse<CampaignResponse>>(`${this.apiUrl}/${id}/restore`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  hardDelete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}?hard=true`);
  }
}
