export interface ChatMessage {
  role: 'user' | 'model';
  content: string;
}

export interface ChatbotRequest {
  message: string;
  chatHistory: ChatMessage[];
}

export interface ChatbotResponse {
  response: string;
  modelUsed: string;
}
