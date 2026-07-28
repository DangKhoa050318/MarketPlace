import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { WishlistItemResponse, WishlistStatusResponse } from '../models/wishlist.model';

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private apiUrl = `${environment.apiUrl}/wishlist`;

  constructor(private http: HttpClient) {}

  add(productId: number): Observable<ApiResponse<WishlistItemResponse>> {
    return this.http.post<ApiResponse<WishlistItemResponse>>(`${this.apiUrl}/${productId}`, {});
  }

  remove(productId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${productId}`);
  }

  status(productId: number): Observable<ApiResponse<WishlistStatusResponse>> {
    return this.http.get<ApiResponse<WishlistStatusResponse>>(`${this.apiUrl}/${productId}/status`);
  }
}
