import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PageResponse } from '../models/api-response.model';
import { WishlistItemResponse, WishlistStatusResponse } from '../models/wishlist.model';

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private apiUrl = `${environment.apiUrl}/wishlist`;

  constructor(private http: HttpClient) {}

  list(page = 0, size = 12): Observable<ApiResponse<PageResponse<WishlistItemResponse>>> {
    return this.http.get<ApiResponse<PageResponse<WishlistItemResponse>>>(this.apiUrl, {
      params: {
        page: page.toString(),
        size: size.toString()
      }
    });
  }

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
