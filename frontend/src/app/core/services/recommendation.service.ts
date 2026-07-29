import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  RecommendationQuery,
  RecommendationResponse
} from '../models/recommendation.model';

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  private apiUrl = `${environment.apiUrl}/recommendations`;
  private sessionStorageKey = 'recently_viewed_session_id';

  constructor(private http: HttpClient) {}

  getRecommendations(
    query: RecommendationQuery
  ): Observable<ApiResponse<RecommendationResponse>> {
    let params = new HttpParams()
      .set('placement', query.placement)
      .set('limit', query.limit ?? 12);
    if (query.productId !== undefined) {
      params = params.set('productId', query.productId);
    }
    if (query.categoryId !== undefined) {
      params = params.set('categoryId', query.categoryId);
    }

    return this.http.get<ApiResponse<RecommendationResponse>>(this.apiUrl, {
      params,
      headers: new HttpHeaders({ 'X-Session-Id': this.sessionId() })
    });
  }

  private sessionId(): string {
    const existing = localStorage.getItem(this.sessionStorageKey);
    if (existing) return existing;

    const generated = typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, character => {
          const random = Math.floor(Math.random() * 16);
          const value = character === 'x' ? random : (random & 0x3) | 0x8;
          return value.toString(16);
        });
    localStorage.setItem(this.sessionStorageKey, generated);
    return generated;
  }
}
