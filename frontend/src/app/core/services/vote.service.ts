import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { VotePayload, VoteResponse } from '../models/vote.model';

@Injectable({ providedIn: 'root' })
export class VoteService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  toggleVote(payload: VotePayload): Observable<ApiResponse<VoteResponse>> {
    return this.http.post<ApiResponse<VoteResponse>>(`${this.apiUrl}/content/vote`, payload);
  }
}
