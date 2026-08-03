import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { OrderService } from '../../../core/services/order.service';

@Component({
  selector: 'app-payment-callback',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule
  ],
  template: `
    <div class="callback-container container">
      <mat-card class="callback-card glass-panel text-center">
        <div class="status-icon-wrapper" [class.success]="isSuccess" [class.cancelled]="!isSuccess">
          <mat-icon class="status-icon">{{ isSuccess ? 'check_circle' : 'cancel' }}</mat-icon>
        </div>

        <h1 class="callback-title text-gradient-cyan">
          {{ isSuccess ? 'Payment Completed Successfully!' : 'Payment Cancelled or Failed' }}
        </h1>

        <p class="callback-subtitle" *ngIf="isSuccess">
          Thank you for your purchase! PayGate has processed your transaction.
        </p>
        <p class="callback-subtitle" *ngIf="!isSuccess">
          Your payment session was cancelled or failed to process.
        </p>

        <div class="transaction-details-box glass-panel" *ngIf="orderId || transactionRef">
          <div class="detail-row" *ngIf="orderId">
            <span class="detail-label">Order Reference:</span>
            <strong class="detail-val">#{{ cleanOrderId(orderId) }}</strong>
          </div>
          <div class="detail-row" *ngIf="transactionRef">
            <span class="detail-label">PayGate Ref:</span>
            <strong class="detail-val text-cyan">{{ transactionRef }}</strong>
          </div>
          <div class="detail-row">
            <span class="detail-label">Payment Status:</span>
            <span class="badge-status" [class.badge-success]="isSuccess" [class.badge-danger]="!isSuccess">
              {{ isSuccess ? 'PAID & CONFIRMED' : 'CANCELLED / UNPAID' }}
            </span>
          </div>
        </div>

        <div class="actions-row">
          <a mat-raised-button color="primary" class="btn-glowing" [routerLink]="['/orders', cleanOrderId(orderId)]" *ngIf="orderId">
            <mat-icon>visibility</mat-icon> View Order Details
          </a>
          <a mat-stroked-button routerLink="/orders">
            <mat-icon>format_list_bulleted</mat-icon> All Orders
          </a>
          <a mat-button routerLink="/products">
            <mat-icon>shopping_bag</mat-icon> Continue Shopping
          </a>
        </div>
      </mat-card>
    </div>
  `,
  styles: [`
    .callback-container {
      padding: 40px 16px;
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 70vh;
    }

    .callback-card {
      padding: 36px 32px;
      max-width: 540px;
      width: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      border-radius: 20px;
    }

    .status-icon-wrapper {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      display: flex;
      justify-content: center;
      align-items: center;
      margin-bottom: 20px;
    }
    .status-icon-wrapper.success {
      background: rgba(16, 185, 129, 0.15);
      color: #10b981;
    }
    .status-icon-wrapper.cancelled {
      background: rgba(239, 68, 68, 0.15);
      color: #ef4444;
    }

    .status-icon {
      font-size: 54px;
      width: 54px;
      height: 54px;
    }

    .callback-title {
      font-size: 1.6rem;
      font-weight: 800;
      margin: 0 0 8px;
    }

    .callback-subtitle {
      color: var(--text-muted);
      font-size: 0.95rem;
      margin-bottom: 24px;
    }

    .transaction-details-box {
      width: 100%;
      padding: 16px 20px;
      border-radius: 12px;
      margin-bottom: 28px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      background: rgba(255, 255, 255, 0.6);
      border: 1px solid rgba(226, 232, 240, 0.8);
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 0.9rem;
    }

    .detail-label {
      color: var(--text-muted);
      font-weight: 600;
    }

    .detail-val {
      font-weight: 700;
      color: var(--text-main);
    }

    .badge-status {
      font-size: 0.75rem;
      font-weight: 800;
      padding: 3px 10px;
      border-radius: 12px;
    }
    .badge-success {
      background: #dcfce7;
      color: #15803d;
    }
    .badge-danger {
      background: #fee2e2;
      color: #b91c1c;
    }

    .actions-row {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      justify-content: center;
      width: 100%;
    }

    .text-cyan {
      color: #0284c7;
    }
  `]
})
export class PaymentCallbackComponent implements OnInit {
  status = 'SUCCESS';
  orderId: string | null = null;
  transactionRef: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService
  ) {}

  get isSuccess(): boolean {
    if (!this.status) return false;
    const s = this.status.toUpperCase();
    return s === 'SUCCESS' || s === 'PROCESSING' || s === 'COMPLETED' || s === 'PAID';
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.status = params['status'] || 'SUCCESS';
      this.orderId = params['orderId'] || null;
      this.transactionRef = params['transactionRef'] || null;

      if (this.isSuccess && this.orderId) {
        this.orderService.confirmPaygatePayment(this.orderId, this.transactionRef || undefined).subscribe({
          next: (res) => console.log('Payment status confirmed via callback:', res),
          error: (err) => console.warn('Could not auto-confirm payment status via callback:', err)
        });
      }
    });
  }

  cleanOrderId(rawId: string | null): string {
    if (!rawId) return '';
    return rawId.replace(/^ORD-/, '');
  }
}
