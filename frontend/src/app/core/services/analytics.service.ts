import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { EMPTY } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AnalyticsEventType, TrackAnalyticsEventRequest } from '../models/analytics-event.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private apiUrl = `${environment.apiUrl}/analytics/events`;
  private sessionStorageKey = 'recently_viewed_session_id';

  constructor(private http: HttpClient) {}

  track(type: AnalyticsEventType, properties: Record<string, unknown> = {}): void {
    const payload: TrackAnalyticsEventRequest = {
      type,
      occurredAt: this.localIsoTimestamp(),
      properties
    };

    this.http.post<ApiResponse<unknown>>(this.apiUrl, payload, {
      headers: new HttpHeaders({ 'X-Session-Id': this.sessionId() })
    }).pipe(
      catchError(() => EMPTY)
    ).subscribe();
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

  private localIsoTimestamp(): string {
    const now = new Date();
    const timezoneOffsetMs = now.getTimezoneOffset() * 60000;
    return new Date(now.getTime() - timezoneOffsetMs).toISOString().slice(0, 19);
  }
}
