import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PageResponse } from '../models/page-response.model';
import { ModerationAuditLog, ModerationFilter, ModerationItem, UpdateModerationStatusPayload } from '../models/moderation.model';
import { ContentType } from '../models/vote.model';

@Injectable({ providedIn: 'root' })
export class ModerationService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getQueue(filter: ModerationFilter): Observable<ApiResponse<PageResponse<ModerationItem>>> {
    let params = new HttpParams()
      .set('page', filter.page ?? 0)
      .set('size', filter.size ?? 10);

    if (filter.targetType) params = params.set('targetType', filter.targetType);
    if (filter.status) params = params.set('status', filter.status);
    if (filter.productId) params = params.set('productId', filter.productId);
    if (filter.startDate) params = params.set('startDate', filter.startDate);
    if (filter.endDate) params = params.set('endDate', filter.endDate);

    return this.http.get<ApiResponse<PageResponse<ModerationItem>>>(`${this.apiUrl}/admin/moderation/queue`, { params });
  }

  updateStatus(payload: UpdateModerationStatusPayload): Observable<ApiResponse<ModerationItem>> {
    return this.http.put<ApiResponse<ModerationItem>>(`${this.apiUrl}/admin/moderation/status`, payload);
  }

  getLogs(targetType: ContentType, targetId: number): Observable<ApiResponse<ModerationAuditLog[]>> {
    const params = new HttpParams()
      .set('targetType', targetType)
      .set('targetId', targetId);
    return this.http.get<ApiResponse<ModerationAuditLog[]>>(`${this.apiUrl}/admin/moderation/logs`, { params });
  }
}
