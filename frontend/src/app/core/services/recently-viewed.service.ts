import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { RecentlyViewedProductResponse } from '../models/recently-viewed.model';

@Injectable({ providedIn: 'root' })
export class RecentlyViewedService {
  private apiUrl = `${environment.apiUrl}/recently-viewed`;
  private sessionStorageKey = 'recently_viewed_session_id';

  constructor(private http: HttpClient) {}

  record(productId: number): Observable<ApiResponse<RecentlyViewedProductResponse>> {
    return this.http.post<ApiResponse<RecentlyViewedProductResponse>>(
      this.apiUrl,
      { productId },
      { headers: this.sessionHeaders() }
    );
  }

  list(limit = 8): Observable<ApiResponse<RecentlyViewedProductResponse[]>> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ApiResponse<RecentlyViewedProductResponse[]>>(this.apiUrl, {
      params,
      headers: this.sessionHeaders()
    });
  }

  clear(): Observable<void> {
    return this.http.delete<void>(this.apiUrl, { headers: this.sessionHeaders() });
  }

  private sessionHeaders(): HttpHeaders {
    return new HttpHeaders({ 'X-Session-Id': this.sessionId() });
  }

  private sessionId(): string {
    const existing = localStorage.getItem(this.sessionStorageKey);
    if (existing) return existing;

    const generated = typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `session-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    localStorage.setItem(this.sessionStorageKey, generated);
    return generated;
  }
}
