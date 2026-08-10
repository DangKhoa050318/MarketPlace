import { fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { Order } from '../models/order.model';
import { ChatPaymentStatusService } from './chat-payment-status.service';
import { OrderService } from './order.service';

describe('ChatPaymentStatusService', () => {
  let orderService: jasmine.SpyObj<OrderService>;
  let service: ChatPaymentStatusService;

  beforeEach(() => {
    localStorage.removeItem('marketplace_chat_pending_payment_order');
    orderService = jasmine.createSpyObj<OrderService>('OrderService', ['getUserOrderById']);
    service = new ChatPaymentStatusService(orderService);
  });

  it('emits success from authoritative paid order state', fakeAsync(() => {
    orderService.getUserOrderById.and.returnValue(of(response(order('PAID', 'CONFIRMED'))));
    let status: string | undefined;

    service.watch(89).subscribe(outcome => status = outcome.status);
    tick(0);

    expect(status).toBe('SUCCESS');
    expect(localStorage.getItem('marketplace_chat_pending_payment_order')).toBeNull();
  }));

  it('emits failure when webhook cancellation reaches the order', fakeAsync(() => {
    orderService.getUserOrderById.and.returnValue(of(response(order('UNPAID', 'CANCELLED'))));
    let status: string | undefined;

    service.watch(89).subscribe(outcome => status = outcome.status);
    tick(0);

    expect(status).toBe('FAILED');
  }));

  it('resumes a pending payment stored before PayGate redirect', fakeAsync(() => {
    localStorage.setItem('marketplace_chat_pending_payment_order', '89');
    orderService.getUserOrderById.and.returnValue(of(response(order('PAID', 'CONFIRMED'))));
    let orderId: number | undefined;

    service.resume()?.subscribe(outcome => orderId = outcome.orderId);
    tick(0);

    expect(orderId).toBe(89);
  }));
});

function response(data: Order): ApiResponse<Order> {
  return {
    success: true,
    message: 'Order',
    timestamp: new Date().toISOString(),
    data
  };
}

function order(paymentStatus: 'PAID' | 'UNPAID', status: 'CONFIRMED' | 'CANCELLED'): Order {
  return {
    id: 89,
    userId: 1,
    userEmail: 'customer@example.com',
    totalAmount: 16300000,
    status,
    paymentMethod: 'CREDIT_CARD',
    paymentStatus,
    items: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
}
