import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ChatbotRequest, ChatbotResponse } from '../models/chatbot.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({
  providedIn: 'root'
})
export class ChatbotService {
  private readonly API_URL = '/api/v1/chatbot/ask';

  constructor(private http: HttpClient) {}

  askQuestion(request: ChatbotRequest): Observable<ChatbotResponse> {
    return this.http.post<ApiResponse<ChatbotResponse>>(this.API_URL, request)
      .pipe(map(response => response.data));
  }
}
