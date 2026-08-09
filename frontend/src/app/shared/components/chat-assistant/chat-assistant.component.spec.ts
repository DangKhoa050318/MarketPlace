import { of } from 'rxjs';
import { ChatMessageResponse } from '../../../core/models/chat-assistant.model';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ChatAssistantService } from '../../../core/services/chat-assistant.service';
import { ChatCartCommandService } from '../../../core/services/chat-cart-command.service';
import { ChatAssistantComponent } from './chat-assistant.component';

describe('ChatAssistantComponent cart commands', () => {
  let component: ChatAssistantComponent;
  let authService: jasmine.SpyObj<AuthService>;
  let cartService: jasmine.SpyObj<CartService>;

  beforeEach(() => {
    const chatService = jasmine.createSpyObj<ChatAssistantService>(
      'ChatAssistantService', ['send', 'clearConversation']);
    const analytics = jasmine.createSpyObj<AnalyticsService>('AnalyticsService', ['track']);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated']);
    cartService = jasmine.createSpyObj<CartService>('CartService', ['addToCart']);
    const router = { url: '/products' };

    component = new ChatAssistantComponent(
      chatService,
      analytics,
      authService,
      cartService,
      new ChatCartCommandService(),
      router as never
    );
    component.messages.push({
      role: 'assistant',
      text: 'Mình gợi ý hai sản phẩm.',
      response: recommendation()
    });
  });

  it('adds the requested recommended SKU to the cart', () => {
    authService.isAuthenticated.and.returnValue(true);
    cartService.addToCart.and.returnValue(of({
      success: true,
      message: 'Added',
      timestamp: new Date().toISOString(),
      data: {
        userId: 1,
        items: [],
        totalAmount: 18000000,
        totalItems: 1
      }
    }));

    component.send('Hãy thêm laptop đầu tiên vào giỏ hàng');

    expect(cartService.addToCart).toHaveBeenCalledOnceWith(101);
    expect(component.messages[component.messages.length - 1].text).toContain('Đã thêm Laptop Alpha');
  });

  it('requires login before mutating the cart', () => {
    authService.isAuthenticated.and.returnValue(false);

    component.send('Thêm sản phẩm thứ hai vào giỏ');

    expect(cartService.addToCart).not.toHaveBeenCalled();
    expect(component.messages[component.messages.length - 1].text).toContain('cần đăng nhập');
  });
});

function recommendation(): ChatMessageResponse {
  return {
    conversationId: 'conversation-1',
    messageId: 'message-1',
    answer: 'Mình gợi ý hai sản phẩm.',
    intents: ['PRODUCT_DISCOVERY'],
    traceId: 'trace-1',
    quickReplies: [],
    products: [
      product(1, 101, 'Laptop Alpha'),
      product(2, 202, 'Laptop Beta')
    ]
  };
}

function product(productId: number, variantId: number, name: string) {
  return {
    productId,
    slug: name.toLowerCase().replace(/ /g, '-'),
    name,
    categoryId: 1,
    categoryName: 'Laptop',
    recommendedVariantId: variantId,
    recommendedVariantName: '16GB / 512GB',
    recommendedVariantPrice: 18000000,
    minPrice: 18000000,
    maxPrice: 19000000,
    availableStock: 5,
    reason: 'Phù hợp nhu cầu'
  };
}
