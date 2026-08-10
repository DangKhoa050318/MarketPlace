import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  ChatMessageRequest,
  ChatMessageResponse,
  ChatPageContext
} from '../models/chat-assistant.model';

@Injectable({ providedIn: 'root' })
export class ChatAssistantService {
  private readonly apiUrl = `${environment.apiUrl}/chat/messages`;
  private readonly conversationKey = 'marketplace_chat_conversation_id';
  private readonly sessionKey = 'recently_viewed_session_id';

  constructor(private http: HttpClient) {}

  send(
    message: string,
    pageContext?: ChatPageContext,
    couponCode?: string
  ): Observable<ApiResponse<ChatMessageResponse>> {
    const payload: ChatMessageRequest = {
      conversationId: localStorage.getItem(this.conversationKey) || undefined,
      message,
      pageContext,
      couponCode
    };
    return this.http.post<ApiResponse<ChatMessageResponse>>(this.apiUrl, payload, {
      headers: new HttpHeaders({ 'X-Session-Id': this.sessionId() })
    }).pipe(
      tap(response => localStorage.setItem(this.conversationKey, response.data.conversationId))
    );
  }

  clearConversation(): void {
    localStorage.removeItem(this.conversationKey);
  }

  private sessionId(): string {
    const existing = localStorage.getItem(this.sessionKey);
    if (existing) return existing;
    const generated = typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, character => {
          const random = Math.floor(Math.random() * 16);
          const value = character === 'x' ? random : (random & 0x3) | 0x8;
          return value.toString(16);
        });
    localStorage.setItem(this.sessionKey, generated);
    return generated;
  }
}
