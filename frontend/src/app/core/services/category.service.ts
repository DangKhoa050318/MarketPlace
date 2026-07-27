import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { CategoryResponse, CreateCategoryRequest, UpdateCategoryRequest } from '../models/category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private apiUrl = `${environment.apiUrl}/categories`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<CategoryResponse[]>> {
    return this.http.get<ApiResponse<CategoryResponse[]>>(this.apiUrl);
  }

  getCategories(): Observable<ApiResponse<CategoryResponse[]>> {
    return this.getAll();
  }

  getById(id: number): Observable<ApiResponse<CategoryResponse>> {
    return this.http.get<ApiResponse<CategoryResponse>>(`${this.apiUrl}/${id}`);
  }

  create(category: CreateCategoryRequest): Observable<ApiResponse<CategoryResponse>> {
    return this.http.post<ApiResponse<CategoryResponse>>(this.apiUrl, category);
  }

  update(id: number, category: UpdateCategoryRequest): Observable<ApiResponse<CategoryResponse>> {
    return this.http.put<ApiResponse<CategoryResponse>>(`${this.apiUrl}/${id}`, category);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
