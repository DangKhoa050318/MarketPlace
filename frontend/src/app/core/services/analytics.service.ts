import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { EMPTY } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  AnalyticsEventContext,
  AnalyticsEventType,
  TrackAnalyticsEventRequest
} from '../models/analytics-event.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private apiUrl = `${environment.apiUrl}/analytics/events`;
  private sessionStorageKey = 'recently_viewed_session_id';

  constructor(private http: HttpClient) {}

  track(
    type: AnalyticsEventType,
    properties: Record<string, unknown> = {},
    context: AnalyticsEventContext = {}
  ): void {
    const payload: TrackAnalyticsEventRequest = {
      eventId: this.randomId(),
      schemaVersion: 1,
      type,
      occurredAt: new Date().toISOString(),
      ...context,
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

    const generated = this.randomId();
    localStorage.setItem(this.sessionStorageKey, generated);
    return generated;
  }

  private randomId(): string {
    return typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, character => {
          const random = Math.floor(Math.random() * 16);
          const value = character === 'x' ? random : (random & 0x3) | 0x8;
          return value.toString(16);
        });
  }
}
