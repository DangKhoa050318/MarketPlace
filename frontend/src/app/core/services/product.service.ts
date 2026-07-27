import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PageResponse } from '../models/page-response.model';
import { ProductResponse, CreateProductRequest, UpdateProductRequest } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private apiUrl = `${environment.apiUrl}/products`;

  constructor(private http: HttpClient) {}

  getProducts(page = 0, size = 10, sort?: string): Observable<ApiResponse<PageResponse<ProductResponse>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (sort) {
      params = params.set('sort', sort);
    }

    return this.http.get<ApiResponse<PageResponse<ProductResponse>>>(this.apiUrl, { params });
  }

  searchProducts(query: string, page = 0, size = 10): Observable<ApiResponse<PageResponse<ProductResponse>>> {
    const params = new HttpParams()
      .set('q', query)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<PageResponse<ProductResponse>>>(`${this.apiUrl}/search`, { params });
  }

  getProductById(id: number): Observable<ApiResponse<ProductResponse>> {
    return this.http.get<ApiResponse<ProductResponse>>(`${this.apiUrl}/${id}`);
  }

  createProduct(product: CreateProductRequest): Observable<ApiResponse<ProductResponse>> {
    return this.http.post<ApiResponse<ProductResponse>>(this.apiUrl, product);
  }

  updateProduct(id: number, product: UpdateProductRequest): Observable<ApiResponse<ProductResponse>> {
    return this.http.put<ApiResponse<ProductResponse>>(`${this.apiUrl}/${id}`, product);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
