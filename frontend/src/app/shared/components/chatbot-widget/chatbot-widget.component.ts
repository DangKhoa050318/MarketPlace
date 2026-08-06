import { Component, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ChatbotService } from '../../../core/services/chatbot.service';
import { ChatMessage, ChatbotRequest } from '../../../core/models/chatbot.model';
import { trigger, state, style, transition, animate } from '@angular/animations';

@Component({
  selector: 'app-chatbot-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './chatbot-widget.component.html',
  styleUrls: ['./chatbot-widget.component.scss'],
  animations: [
    trigger('windowAnimation', [
      state('void', style({ opacity: 0, transform: 'scale(0.8) translateY(20px)' })),
      state('*', style({ opacity: 1, transform: 'scale(1) translateY(0)' })),
      transition('void => *', animate('200ms cubic-bezier(0.25, 0.8, 0.25, 1)')),
      transition('* => void', animate('150ms cubic-bezier(0.25, 0.8, 0.25, 1)'))
    ])
  ]
})
export class ChatbotWidgetComponent implements AfterViewChecked {
  @ViewChild('scrollContainer') private scrollContainer!: ElementRef;

  isOpen = false;
  isLoading = false;
  messages: ChatMessage[] = [];
  userInput = '';

  constructor(private chatbotService: ChatbotService) {
    // Initial greeting
    this.messages.push({
      role: 'model',
      content: 'Chào bạn! Mình là AI Assistant của MarketPlace. Mình có thể giúp gì cho bạn hôm nay (tìm sản phẩm, tư vấn mã giảm giá...)?'
    });
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
  }

  sendMessage() {
    if (!this.userInput.trim() || this.isLoading) return;

    const userText = this.userInput.trim();
    this.userInput = '';
    
    // Add user message to UI
    const newUserMessage: ChatMessage = { role: 'user', content: userText };
    this.messages.push(newUserMessage);
    
    this.isLoading = true;

    // Prepare request (exclude the first greeting or just send recent history)
    const request: ChatbotRequest = {
      message: userText,
      chatHistory: this.messages.slice(0, -1).map(m => ({
        role: m.role,
        content: m.content
      }))
    };

    this.chatbotService.askQuestion(request).subscribe({
      next: (res) => {
        this.messages.push({
          role: 'model',
          content: res.response
        });
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Chatbot error:', err);
        this.messages.push({
          role: 'model',
          content: 'Xin lỗi, hiện tại mình đang gặp sự cố kết nối. Vui lòng thử lại sau.'
        });
        this.isLoading = false;
      }
    });
  }

  // Simple Markdown to HTML converter for basic formatting (bold and lists)
  formatMessage(text: string): string {
    if (!text) return '';
    let html = text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/\n/g, '<br/>');
    
    // Handle simple bullet points
    html = html.replace(/(?:<br\/>)*- (.*?)(?:<br\/>|$)/g, '<li>$1</li>');
    if (html.includes('<li>')) {
       html = html.replace(/(<li>.*<\/li>)/g, '<ul>$1</ul>');
    }
    return html;
  }

  private scrollToBottom(): void {
    try {
      if (this.scrollContainer) {
        this.scrollContainer.nativeElement.scrollTop = this.scrollContainer.nativeElement.scrollHeight;
      }
    } catch(err) { }
  }
}
