import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { ChatAssistantService } from './chat-assistant.service';

describe('ChatAssistantService', () => {
  let service: ChatAssistantService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ChatAssistantService]
    });
    service = TestBed.inject(ChatAssistantService);
    http = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('sends an anonymous session and persists the conversation id', () => {
    service.send('Laptop bán chạy', { categoryId: 3 }).subscribe();

    const request = http.expectOne(`${environment.apiUrl}/chat/messages`);
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('X-Session-Id')).toBeTruthy();
    expect(request.request.body.message).toBe('Laptop bán chạy');
    expect(request.request.body.pageContext.categoryId).toBe(3);
    request.flush({
      success: true,
      message: 'ok',
      timestamp: new Date().toISOString(),
      data: {
        conversationId: 'conversation-1',
        messageId: 'message-1',
        answer: 'answer',
        intents: ['BEST_SELLER'],
        products: [],
        quickReplies: [],
        traceId: 'trace-1'
      }
    });

    expect(localStorage.getItem('marketplace_chat_conversation_id')).toBe('conversation-1');
  });

  it('reuses the stored conversation id on the next message', () => {
    localStorage.setItem('marketplace_chat_conversation_id', 'conversation-1');
    service.send('Có voucher không?').subscribe();

    const request = http.expectOne(`${environment.apiUrl}/chat/messages`);
    expect(request.request.body.conversationId).toBe('conversation-1');
    request.flush({
      success: true,
      message: 'ok',
      timestamp: new Date().toISOString(),
      data: {
        conversationId: 'conversation-1', messageId: 'message-2', answer: 'answer',
        intents: ['CAMPAIGN_OFFERS'], products: [], quickReplies: [], traceId: 'trace-2'
      }
    });
  });
});
