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
      <mat-card class="callback-card text-center">
        <div class="status-icon-wrapper" [class.success]="isSuccess" [class.cancelled]="!isSuccess">
          <mat-icon class="status-icon">{{ isSuccess ? 'check_circle' : 'cancel' }}</mat-icon>
        </div>

        <h1 class="callback-title" [class.title-success]="isSuccess" [class.title-cancelled]="!isSuccess">
          {{ isSuccess ? 'Payment Completed Successfully!' : 'Payment Cancelled or Failed' }}
        </h1>

        <p class="callback-subtitle" *ngIf="isSuccess">
          Thank you for your purchase! PayGate has processed your transaction.
        </p>
        <p class="callback-subtitle" *ngIf="!isSuccess">
          Your payment session was cancelled or failed to process.
        </p>

        <div class="transaction-details-box" *ngIf="orderId || transactionRef">
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
          <a mat-raised-button color="primary" class="action-btn btn-primary" [routerLink]="['/orders', cleanOrderId(orderId)]" *ngIf="orderId">
            <mat-icon class="btn-icon">visibility</mat-icon> View Order Details
          </a>
          <a mat-stroked-button class="action-btn" routerLink="/orders">
            <mat-icon class="btn-icon">format_list_bulleted</mat-icon> All Orders
          </a>
          <a mat-button class="action-btn" routerLink="/products">
            <mat-icon class="btn-icon">shopping_bag</mat-icon> Continue Shopping
          </a>
        </div>
      </mat-card>
    </div>
  `,
  styles: [`
    .callback-container {
      padding: 60px 16px;
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 75vh;
      background-color: #f8fafc;
    }

    .callback-card {
      padding: 40px 32px;
      max-width: 540px;
      width: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      border-radius: 16px;
      background: #ffffff !important;
      color: #0f172a !important;
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04) !important;
      border: 1px solid #e2e8f0 !important;
    }

    .status-icon-wrapper {
      width: 76px;
      height: 76px;
      border-radius: 50%;
      display: flex;
      justify-content: center;
      align-items: center;
      margin-bottom: 20px;
    }
    .status-icon-wrapper.success {
      background: #dcfce7;
      color: #16a34a;
    }
    .status-icon-wrapper.cancelled {
      background: #fee2e2;
      color: #dc2626;
    }

    .status-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      line-height: 48px;
    }

    .callback-title {
      font-size: 1.5rem;
      font-weight: 700;
      margin: 0 0 10px;
      line-height: 1.3;
    }
    .title-success {
      color: #15803d;
    }
    .title-cancelled {
      color: #b91c1c;
    }

    .callback-subtitle {
      color: #64748b;
      font-size: 0.95rem;
      margin-bottom: 24px;
      line-height: 1.5;
    }

    .transaction-details-box {
      width: 100%;
      padding: 18px 20px;
      border-radius: 12px;
      margin-bottom: 28px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      box-sizing: border-box;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 0.9rem;
    }

    .detail-label {
      color: #64748b;
      font-weight: 600;
    }

    .detail-val {
      font-weight: 700;
      color: #0f172a;
    }

    .badge-status {
      font-size: 0.75rem;
      font-weight: 800;
      padding: 4px 12px;
      border-radius: 12px;
      letter-spacing: 0.3px;
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
      flex-direction: column;
      gap: 10px;
      width: 100%;
    }

    @media (min-width: 480px) {
      .actions-row {
        flex-direction: row;
        flex-wrap: wrap;
        justify-content: center;
      }
    }

    .action-btn {
      display: inline-flex !important;
      align-items: center !important;
      justify-content: center !important;
      gap: 6px;
      padding: 0 16px !important;
      height: 40px !important;
      font-weight: 600 !important;
    }

    .btn-icon {
      font-size: 18px !important;
      width: 18px !important;
      height: 18px !important;
      line-height: 18px !important;
      margin: 0 !important;
    }

    .text-cyan {
      color: #0284c7;
    }
  `]
})
export class PaymentCallbackComponent implements OnInit {
  /** Initial hint from PayGate redirect query params; may be overridden by server truth. */
  status = 'SUCCESS';
  orderId: string | null = null;
  transactionRef: string | null = null;
  /** True while we're polling the server for the real order status. */
  loading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService
  ) {}

  private getFirstString(val: any): string | null {
    if (!val) return null;
    if (Array.isArray(val)) {
      return val.length > 0 ? String(val[0]) : null;
    }
    return String(val);
  }

  get isSuccess(): boolean {
    const raw = this.getFirstString(this.status);
    if (!raw) return false;
    const s = raw.toUpperCase();
    return s === 'SUCCESS' || s === 'PROCESSING' || s === 'COMPLETED' || s === 'PAID';
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const urlStatus = this.getFirstString(params['status']) || 'CANCELLED';
      this.orderId = this.getFirstString(params['orderId']);
      this.transactionRef = this.getFirstString(params['transactionRef']);

      // Instead of trusting the URL's SUCCESS status, we default to PROCESSING
      // if it claims success, and wait for the backend to confirm.
      if (urlStatus.toUpperCase() === 'SUCCESS') {
         this.status = 'PROCESSING';
      } else {
         this.status = urlStatus;
      }

      if (this.orderId) {
        const numericId = this.cleanOrderId(this.orderId);
        if (urlStatus.toUpperCase() === 'CANCELLED' && numericId) {
          // Proactively cancel the order on the backend so it transitions from PENDING
          // to CANCELLED and reserved stock is released.
          this.orderService.cancelPayment(Number(numericId)).subscribe({
            next: () => this.pollOrderStatus(numericId, 0),
            error: () => this.pollOrderStatus(numericId, 0)
          });
        } else {
          this.pollOrderStatus(numericId, 0);
        }
      }
    });
  }

  /**
   * Polls the order detail GET endpoint up to 3 times (with 2-second intervals)
   * to read the payment status that was set by the server-to-server webhook.
   * The initial UI uses the PayGate redirect hint; once the server responds
   * with a terminal status, the UI is updated to reflect the truth.
   */
  private pollOrderStatus(numericId: string, attempt: number): void {
    const id = Number(numericId);
    if (isNaN(id) || id <= 0) return;

    this.loading = attempt === 0;
    this.orderService.getUserOrderById(id).subscribe({
      next: (res) => {
        this.loading = false;
        if (res?.data) {
          const ps = res.data.paymentStatus?.toUpperCase();
          if (ps === 'PAID') {
            this.status = 'SUCCESS';
            this.transactionRef = res.data.paygateTransactionRef || this.transactionRef;
          } else if (ps === 'UNPAID' && res.data.status?.toUpperCase() === 'CANCELLED') {
            this.status = 'CANCELLED';
          } else if (attempt < 3) {
            // Webhook may not have arrived yet — retry after a short delay
            setTimeout(() => this.pollOrderStatus(numericId, attempt + 1), 2000);
          }
          // else: keep the initial hint from query params
        }
      },
      error: () => {
        this.loading = false;
        // Keep the initial hint from query params on error
      }
    });
  }

  cleanOrderId(rawId: any): string {
    const str = this.getFirstString(rawId);
    if (!str) return '';
    return str.replace(/^ORD-/, '');
  }
}
