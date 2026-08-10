import { Injectable } from '@angular/core';
import { EMPTY, Observable, catchError, filter, map, of, switchMap, take, tap, timeout, timer } from 'rxjs';
import { Order } from '../models/order.model';
import { OrderService } from './order.service';

export type ChatPaymentOutcomeStatus = 'SUCCESS' | 'FAILED' | 'PROCESSING';

export interface ChatPaymentOutcome {
  status: ChatPaymentOutcomeStatus;
  orderId: number;
  order?: Order;
}

@Injectable({ providedIn: 'root' })
export class ChatPaymentStatusService {
  private readonly pendingOrderKey = 'marketplace_chat_pending_payment_order';
  private readonly pollIntervalMs = 2000;
  private readonly pollTimeoutMs = 5 * 60 * 1000;

  constructor(private orderService: OrderService) {}

  watch(orderId: number): Observable<ChatPaymentOutcome> {
    localStorage.setItem(this.pendingOrderKey, String(orderId));
    return this.poll(orderId);
  }

  resume(): Observable<ChatPaymentOutcome> | null {
    const stored = Number(localStorage.getItem(this.pendingOrderKey));
    if (!Number.isInteger(stored) || stored <= 0) {
      localStorage.removeItem(this.pendingOrderKey);
      return null;
    }
    return this.poll(stored);
  }

  private poll(orderId: number): Observable<ChatPaymentOutcome> {
    return timer(0, this.pollIntervalMs).pipe(
      switchMap(() => this.orderService.getUserOrderById(orderId).pipe(
        catchError(() => EMPTY)
      )),
      map(response => this.terminalOutcome(response.data)),
      filter((outcome): outcome is ChatPaymentOutcome => outcome !== null),
      take(1),
      timeout({ first: this.pollTimeoutMs }),
      catchError(() => of<ChatPaymentOutcome>({ status: 'PROCESSING', orderId })),
      tap(outcome => {
        if (outcome.status !== 'PROCESSING') {
          localStorage.removeItem(this.pendingOrderKey);
        }
      })
    );
  }

  private terminalOutcome(order: Order): ChatPaymentOutcome | null {
    if (order.paymentStatus === 'PAID') {
      return { status: 'SUCCESS', orderId: order.id, order };
    }
    if (order.status === 'CANCELLED') {
      return { status: 'FAILED', orderId: order.id, order };
    }
    return null;
  }
}
