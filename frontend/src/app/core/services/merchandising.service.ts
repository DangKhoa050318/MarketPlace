import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { EMPTY, Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { CampaignResponse } from '../models/campaign.model';
import { CollectionResponse } from '../models/collection.model';
import { BannerResponse } from '../models/banner.model';
import {
  MerchandisingEventType,
  MerchandisingSummaryResponse,
  MerchandisingTargetType,
  RecordMerchandisingEventRequest
} from '../models/merchandising.model';

/**
 * Storefront-facing merchandising API: reads the server-computed "effective" campaigns/collections/
 * banners (visibility is decided by the backend, B-405) and records impression/click events
 * (B-407). Also exposes the admin effectiveness summary (B-408) for F-406.
 *
 * Event recording is fire-and-forget and idempotent server-side by {@code eventId}; it never
 * surfaces an error to the storefront. The session id is shared with {@code AnalyticsService} so
 * anonymous last-click attribution can line up across features.
 */
@Injectable({ providedIn: 'root' })
export class MerchandisingService {
  private base = environment.apiUrl;
  private sessionStorageKey = 'recently_viewed_session_id';

  constructor(private http: HttpClient) {}

  effectiveCampaigns(): Observable<ApiResponse<CampaignResponse[]>> {
    return this.http.get<ApiResponse<CampaignResponse[]>>(`${this.base}/campaigns`);
  }

  publishedCollections(): Observable<ApiResponse<CollectionResponse[]>> {
    return this.http.get<ApiResponse<CollectionResponse[]>>(`${this.base}/collections`);
  }

  collectionBySlug(slug: string): Observable<ApiResponse<CollectionResponse>> {
    return this.http.get<ApiResponse<CollectionResponse>>(
      `${this.base}/collections/${encodeURIComponent(slug)}`
    );
  }

  effectiveBanners(position: string): Observable<ApiResponse<BannerResponse[]>> {
    const params = new HttpParams().set('position', position);
    return this.http.get<ApiResponse<BannerResponse[]>>(`${this.base}/banners`, { params });
  }

  /** Fire-and-forget impression/click. Idempotent server-side by eventId; swallows all errors. */
  recordEvent(
    eventType: MerchandisingEventType,
    targetType: MerchandisingTargetType,
    targetId: number
  ): void {
    const payload: RecordMerchandisingEventRequest = {
      eventId: this.randomId(),
      eventType,
      targetType,
      targetId,
      sessionId: this.sessionId()
    };
    this.http
      .post<ApiResponse<void>>(`${this.base}/merchandising/events`, payload)
      .pipe(catchError(() => EMPTY))
      .subscribe();
  }

  /** Effectiveness of a target over a time range (ADMIN, B-408). `from`/`to` are ISO local date-times. */
  summary(
    targetType: MerchandisingTargetType,
    targetId: number,
    from: string,
    to: string
  ): Observable<ApiResponse<MerchandisingSummaryResponse>> {
    const params = new HttpParams()
      .set('targetType', targetType)
      .set('targetId', targetId.toString())
      .set('from', from)
      .set('to', to);
    return this.http.get<ApiResponse<MerchandisingSummaryResponse>>(
      `${this.base}/admin/merchandising/summary`,
      { params }
    );
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
