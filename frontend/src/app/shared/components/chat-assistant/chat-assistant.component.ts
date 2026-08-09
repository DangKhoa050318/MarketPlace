import { CommonModule } from '@angular/common';
import { Component, ElementRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ChatProductCard, ChatMessageResponse } from '../../../core/models/chat-assistant.model';
import { AnalyticsEventSource, AnalyticsEventType } from '../../../core/models/analytics-event.model';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ChatAssistantService } from '../../../core/services/chat-assistant.service';
import { ChatCartCommand, ChatCartCommandService } from '../../../core/services/chat-cart-command.service';

interface UiMessage {
  role: 'user' | 'assistant';
  text: string;
  response?: ChatMessageResponse;
}

@Component({
  selector: 'app-chat-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule, RouterLink],
  templateUrl: './chat-assistant.component.html',
  styleUrls: ['./chat-assistant.component.scss']
})
export class ChatAssistantComponent {
  @ViewChild('messageViewport') private viewport?: ElementRef<HTMLElement>;

  open = false;
  loading = false;
  draft = '';
  messages: UiMessage[] = [{
    role: 'assistant',
    text: 'Xin chào! Mình có thể tìm sản phẩm bán chạy, tư vấn theo nhu cầu hoặc tìm voucher đang có hiệu lực.'
  }];

  constructor(
    private chatService: ChatAssistantService,
    private analytics: AnalyticsService,
    private authService: AuthService,
    private cartService: CartService,
    private cartCommandService: ChatCartCommandService,
    private router: Router
  ) {}

  toggle(): void {
    this.open = !this.open;
    if (this.open) this.scrollSoon();
  }

  send(suggested?: string): void {
    const message = (suggested || this.draft).trim();
    if (!message || this.loading) return;

    this.messages.push({ role: 'user', text: message });
    this.draft = '';
    this.scrollSoon();

    const cartCommand = this.cartCommandService.parse(message);
    if (cartCommand) {
      this.handleCartCommand(cartCommand);
      return;
    }

    this.loading = true;
    this.chatService.send(message, this.pageContext()).pipe(
      finalize(() => {
        this.loading = false;
        this.scrollSoon();
      })
    ).subscribe({
      next: response => {
        this.messages.push({
          role: 'assistant',
          text: response.data.answer,
          response: response.data
        });
        response.data.products.forEach(product => this.analytics.track(
          AnalyticsEventType.ChatProductImpression,
          { conversationId: response.data.conversationId, traceId: response.data.traceId },
          { productId: product.productId, source: AnalyticsEventSource.ChatAssistant }
        ));
      },
      error: () => this.messages.push({
        role: 'assistant',
        text: 'Mình chưa thể xử lý yêu cầu lúc này. Bạn vui lòng thử lại sau nhé.'
      })
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  trackProduct(product: ChatProductCard, response: ChatMessageResponse): void {
    this.analytics.track(
      AnalyticsEventType.ChatProductClick,
      { conversationId: response.conversationId, traceId: response.traceId },
      { productId: product.productId, source: AnalyticsEventSource.ChatAssistant }
    );
  }

  copyVoucher(product: ChatProductCard, response: ChatMessageResponse, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (!product.voucher) return;
    navigator.clipboard?.writeText(product.voucher.code);
    this.analytics.track(
      AnalyticsEventType.ChatVoucherClick,
      {
        conversationId: response.conversationId,
        campaignId: product.voucher.campaignId,
        voucherCode: product.voucher.code
      },
      { productId: product.productId, source: AnalyticsEventSource.ChatAssistant }
    );
  }

  reset(): void {
    this.chatService.clearConversation();
    this.messages = [{
      role: 'assistant',
      text: 'Đã bắt đầu cuộc trò chuyện mới. Bạn muốn tìm sản phẩm như thế nào?'
    }];
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency', currency: 'VND', maximumFractionDigits: 0
    }).format(value);
  }

  private handleCartCommand(command: ChatCartCommand): void {
    const recommendation = this.latestRecommendation();
    if (!recommendation) {
      this.reply('Mình chưa có danh sách sản phẩm gần đây. Bạn hãy yêu cầu mình gợi ý sản phẩm trước nhé.');
      return;
    }

    const products = recommendation.products;
    if (!command.position && products.length > 1) {
      this.reply(`Danh sách gần nhất có ${products.length} sản phẩm. Bạn muốn thêm sản phẩm thứ mấy vào giỏ hàng?`);
      return;
    }

    const position = command.position || 1;
    const product = products[position - 1];
    if (!product) {
      this.reply(`Danh sách gần nhất chỉ có ${products.length} sản phẩm. Bạn vui lòng chọn vị trí từ 1 đến ${products.length}.`);
      return;
    }

    if (!this.authService.isAuthenticated()) {
      this.reply('Bạn cần đăng nhập trước khi thêm sản phẩm vào giỏ hàng. Danh sách gợi ý vẫn được giữ để bạn chọn lại sau khi đăng nhập.');
      return;
    }

    this.loading = true;
    this.cartService.addToCart(product.recommendedVariantId).pipe(
      finalize(() => {
        this.loading = false;
        this.scrollSoon();
      })
    ).subscribe({
      next: response => {
        if (!response.success) {
          this.reply('Mình chưa thể thêm sản phẩm vào giỏ hàng lúc này. Bạn vui lòng thử lại nhé.');
          return;
        }
        this.reply(
          `Đã thêm ${product.name} (${product.recommendedVariantName}) vào giỏ hàng với giá ${this.formatPrice(product.recommendedVariantPrice)}.`
        );
        this.analytics.track(
          AnalyticsEventType.AddToCart,
          {
            conversationId: recommendation.conversationId,
            traceId: recommendation.traceId,
            initiatedBy: 'CHAT_COMMAND'
          },
          {
            productId: product.productId,
            variantId: product.recommendedVariantId,
            quantity: 1,
            unitPrice: product.recommendedVariantPrice,
            source: AnalyticsEventSource.ChatAssistant
          }
        );
      },
      error: () => this.reply(
        'Mình chưa thể thêm sản phẩm vào giỏ hàng. Bạn hãy kiểm tra lại đăng nhập hoặc thử lại sau nhé.'
      )
    });
  }

  private latestRecommendation(): ChatMessageResponse | undefined {
    return [...this.messages]
      .reverse()
      .find(message => message.response?.products.length)?.response;
  }

  private reply(text: string): void {
    this.messages.push({ role: 'assistant', text });
    this.scrollSoon();
  }

  private pageContext(): { productId?: number } | undefined {
    const match = this.router.url.match(/^\/products\/(\d+)/);
    return match ? { productId: Number(match[1]) } : undefined;
  }

  private scrollSoon(): void {
    setTimeout(() => {
      const element = this.viewport?.nativeElement;
      if (element) element.scrollTop = element.scrollHeight;
    });
  }
}
