import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PageResponse } from '../models/page-response.model';
import { CreateAnswerPayload, CreateQuestionPayload, ProductAnswer, ProductQuestion } from '../models/question.model';

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getQuestions(productId: number, page = 0, size = 10): Observable<ApiResponse<PageResponse<ProductQuestion>>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);
    return this.http.get<ApiResponse<PageResponse<ProductQuestion>>>(
      `${this.apiUrl}/products/${productId}/questions`,
      { params }
    );
  }

  askQuestion(productId: number, payload: CreateQuestionPayload): Observable<ApiResponse<ProductQuestion>> {
    return this.http.post<ApiResponse<ProductQuestion>>(
      `${this.apiUrl}/products/${productId}/questions`,
      payload
    );
  }

  answerQuestion(questionId: number, payload: CreateAnswerPayload): Observable<ApiResponse<ProductAnswer>> {
    return this.http.post<ApiResponse<ProductAnswer>>(
      `${this.apiUrl}/questions/${questionId}/answers`,
      payload
    );
  }

  markOfficial(answerId: number): Observable<ApiResponse<ProductAnswer>> {
    return this.http.post<ApiResponse<ProductAnswer>>(
      `${this.apiUrl}/answers/${answerId}/official`,
      {}
    );
  }
}
