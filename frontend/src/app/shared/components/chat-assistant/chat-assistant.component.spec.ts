import { of } from 'rxjs';
import { ChatMessageResponse } from '../../../core/models/chat-assistant.model';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ChatAssistantService } from '../../../core/services/chat-assistant.service';
import { ChatCartCommandService } from '../../../core/services/chat-cart-command.service';
import { ChatPaymentStatusService } from '../../../core/services/chat-payment-status.service';
import { ChatVoucherCommandService } from '../../../core/services/chat-voucher-command.service';
import { PromotionService } from '../../../core/services/promotion.service';
import { ChatAssistantComponent } from './chat-assistant.component';

describe('ChatAssistantComponent cart commands', () => {
  let component: ChatAssistantComponent;
  let chatService: jasmine.SpyObj<ChatAssistantService>;
  let authService: jasmine.SpyObj<AuthService>;
  let cartService: jasmine.SpyObj<CartService>;
  let promotionService: jasmine.SpyObj<PromotionService>;
  let paymentStatusService: jasmine.SpyObj<ChatPaymentStatusService>;

  beforeEach(() => {
    chatService = jasmine.createSpyObj<ChatAssistantService>(
      'ChatAssistantService', ['send', 'clearConversation']);
    const analytics = jasmine.createSpyObj<AnalyticsService>('AnalyticsService', ['track']);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated']);
    cartService = jasmine.createSpyObj<CartService>(
      'CartService', ['addToCart', 'markCheckoutComplete']);
    promotionService = jasmine.createSpyObj<PromotionService>(
      'PromotionService', ['apply', 'appliedCouponCode']);
    paymentStatusService = jasmine.createSpyObj<ChatPaymentStatusService>(
      'ChatPaymentStatusService', ['watch', 'resume']);
    promotionService.appliedCouponCode.and.returnValue(undefined);
    paymentStatusService.resume.and.returnValue(null);
    const router = { url: '/products' };

    component = new ChatAssistantComponent(
      chatService,
      analytics,
      authService,
      cartService,
      new ChatCartCommandService(),
      new ChatVoucherCommandService(),
      promotionService,
      paymentStatusService,
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

  it('applies the single voucher from the latest recommendation', () => {
    authService.isAuthenticated.and.returnValue(true);
    promotionService.apply.and.returnValue(of({
      success: true,
      message: 'Valid coupon',
      timestamp: new Date().toISOString(),
      data: {
        code: 'SCHOOL10',
        valid: true,
        discountType: 'PERCENT',
        eligibleSubtotal: 18000000,
        discountAmount: 1800000,
        cartSubtotal: 18000000,
        newTotal: 16200000
      }
    }));

    component.send('Áp dụng voucher này vào giỏ hàng');

    expect(promotionService.apply).toHaveBeenCalledOnceWith('SCHOOL10');
    expect(component.messages[component.messages.length - 1].text).toContain('Đã áp dụng voucher SCHOOL10');
  });

  it('updates local cart state when COD checkout completes in chat', () => {
    promotionService.appliedCouponCode.and.returnValue('SCHOOL10');
    chatService.send.and.returnValue(of({
      success: true,
      message: 'Order created',
      timestamp: new Date().toISOString(),
      data: {
        conversationId: 'conversation-1',
        messageId: 'message-checkout',
        answer: 'Đặt hàng COD thành công.',
        intents: ['CHECKOUT'],
        products: [],
        quickReplies: [],
        traceId: 'trace-checkout',
        order: {
          id: 88,
          status: 'CONFIRMED',
          paymentMethod: 'COD',
          paymentStatus: 'UNPAID',
          totalAmount: 16300000,
          discountAmount: 1800000,
          shippingFee: 100000,
          couponCode: 'SCHOOL10',
          shippingAddress: '123 Nguyễn Trãi, Quận 5, TP.HCM',
          itemCount: 1,
          createdAt: new Date().toISOString()
        }
      }
    }));

    component.send('Bỏ qua ghi chú');

    expect(chatService.send).toHaveBeenCalledWith(
      'Bỏ qua ghi chú', undefined, 'SCHOOL10');
    expect(cartService.markCheckoutComplete).toHaveBeenCalled();
    expect(component.messages[component.messages.length - 1].response?.order?.id).toBe(88);
  });

  it('writes successful PayGate confirmation back into the chat', () => {
    paymentStatusService.watch.and.returnValue(of({
      status: 'SUCCESS',
      orderId: 89,
      order: {
        id: 89,
        userId: 1,
        userEmail: 'customer@example.com',
        totalAmount: 16300000,
        status: 'CONFIRMED',
        paymentMethod: 'CREDIT_CARD',
        paymentStatus: 'PAID',
        paygateTransactionRef: 'TXN-CARD-89',
        items: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    }));

    component.watchPaygatePayment({
      id: 89,
      status: 'PENDING',
      paymentMethod: 'CREDIT_CARD',
      paymentStatus: 'PENDING_PAYGATE',
      totalAmount: 16300000,
      discountAmount: 1800000,
      shippingFee: 100000,
      shippingAddress: '123 Nguyễn Trãi, Quận 5, TP.HCM',
      itemCount: 1,
      paymentUrl: 'http://localhost:4201/checkout?token=CHK_CARD',
      createdAt: new Date().toISOString()
    });

    expect(paymentStatusService.watch).toHaveBeenCalledOnceWith(89);
    expect(component.open).toBeTrue();
    expect(component.messages[component.messages.length - 1].text)
      .withContext('payment result should be visible in chat')
      .toContain('đã thành công');
    expect(component.messages[component.messages.length - 1].text).toContain('TXN-CARD-89');
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
      product(1, 101, 'Laptop Alpha', true),
      product(2, 202, 'Laptop Beta')
    ]
  };
}

function product(productId: number, variantId: number, name: string, withVoucher = false) {
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
    reason: 'Phù hợp nhu cầu',
    voucher: withVoucher ? {
      campaignId: 1,
      campaignName: 'Back to school',
      code: 'SCHOOL10',
      discountType: 'PERCENT' as const,
      discountValue: 10,
      minOrderAmount: 0,
      scopeType: 'PRODUCT' as const,
      discountText: 'Giảm 10%'
    } : undefined
  };
}
