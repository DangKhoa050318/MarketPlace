import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PageResponse } from '../models/page-response.model';
import {
  ProductResponse,
  StorefrontProduct,
  ProductCatalogQuery,
  ProductVariant,
  CreateProductRequest,
  UpdateProductRequest,
  CreateProductVariantRequest,
  UpdateProductVariantRequest
} from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private apiUrl = `${environment.apiUrl}/products`;
  private variantApiUrl = `${environment.apiUrl}/variants`;

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

  browseProducts(query: ProductCatalogQuery): Observable<ApiResponse<PageResponse<StorefrontProduct>>> {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('sortBy', query.sortBy)
      .set('sortDir', query.sortDir);
    if (query.q) params = params.set('q', query.q);
    if (query.categoryId !== undefined) params = params.set('categoryId', query.categoryId);
    if (query.minPrice !== undefined) params = params.set('minPrice', query.minPrice);
    if (query.maxPrice !== undefined) params = params.set('maxPrice', query.maxPrice);
    if (query.inStock) params = params.set('inStock', true);
    return this.http.get<ApiResponse<PageResponse<StorefrontProduct>>>(`${this.apiUrl}/catalog`, { params });
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

  // --- Product Variant API Methods ---

  getVariants(productId: number): Observable<ApiResponse<ProductVariant[]>> {
    return this.http.get<ApiResponse<ProductVariant[]>>(`${this.apiUrl}/${productId}/variants`);
  }

  createVariant(productId: number, variant: CreateProductVariantRequest): Observable<ApiResponse<ProductVariant>> {
    return this.http.post<ApiResponse<ProductVariant>>(`${this.apiUrl}/${productId}/variants`, variant);
  }

  updateVariant(variantId: number, variant: UpdateProductVariantRequest): Observable<ApiResponse<ProductVariant>> {
    return this.http.put<ApiResponse<ProductVariant>>(`${this.variantApiUrl}/${variantId}`, variant);
  }

  deleteVariant(variantId: number): Observable<void> {
    return this.http.delete<void>(`${this.variantApiUrl}/${variantId}`);
  }
}
