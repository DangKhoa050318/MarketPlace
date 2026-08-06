import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { OrderService, Order } from '../../../core/services/order.service';
import { ReviewService } from '../../../core/services/review.service';
import { NotificationService } from '../../../core/services/notification.service';
import { DeliveryService } from '../../../core/services/delivery.service';
import { Delivery } from '../../../core/models/delivery.model';
import { DeliveryTimelineComponent } from '../../../shared/components/delivery-timeline/delivery-timeline.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { catchError, finalize, of, switchMap, tap } from 'rxjs';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    DeliveryTimelineComponent
  ],
  template: `
    <div class="order-detail-container">
      <div class="header-actions">
        <a mat-button class="back-btn" routerLink="/orders">
          <mat-icon>arrow_back</mat-icon> Back to My Orders
        </a>
      </div>

      <div *ngIf="loading" class="loading-container">
        <mat-spinner diameter="44"></mat-spinner>
      </div>

      <div *ngIf="!loading && order" class="order-content">
        <!-- Summary Header Card -->
        <div class="order-summary-header glass-panel">
          <div class="title-status-row">
            <div>
              <h1 class="order-title">Order #{{ order.id }}</h1>
              <p class="order-date">Placed on {{ order.createdAt | date:'medium' }}</p>
            </div>

            <div class="status-actions">
              <span class="badge-pill" [ngClass]="getStatusBadgeClass(order.status)">
                {{ order.status }}
              </span>
              <button *ngIf="canCancelOrder()" mat-stroked-button color="warn" class="confirm-btn"
                      (click)="cancelOrder()" [disabled]="cancelling">
                <mat-icon>cancel</mat-icon>
                {{ cancelling ? 'Cancelling...' : cancelButtonLabel() }}
              </button>
              <button *ngIf="canRequestReturn()" mat-stroked-button color="accent" class="confirm-btn"
                      (click)="openReturnRequestForm()" [disabled]="requestingReturn">
                <mat-icon>assignment_return</mat-icon>
                Return request
              </button>
              <button *ngIf="canConfirmReceived()" mat-flat-button color="primary" class="confirm-btn"
                      (click)="confirmReceived()" [disabled]="confirming">
                <mat-icon>check_circle</mat-icon>
                {{ confirming ? 'Đang xác nhận...' : 'Đã nhận hàng' }}
              </button>
            </div>
          </div>

          <form *ngIf="showReturnRequestForm" class="return-request-form" (ngSubmit)="createReturnRequest()">
            <label for="return-request-item">Item to return</label>
            <select
              id="return-request-item"
              name="returnRequestItem"
              [(ngModel)]="returnOrderItemId"
              (ngModelChange)="useMaxReturnQuantity()">
              <option [ngValue]="null">Entire order</option>
              <option *ngFor="let item of refundableItems()" [ngValue]="item.id">
                {{ item.productName }} - refundable quantity {{ refundableQuantity(item) }}
              </option>
            </select>

            <div class="return-quantity-summary">
              <strong>Return quantity</strong>
              <span>{{ returnQuantityLabel() }}</span>
            </div>

            <label for="return-request-reason">Return request reason</label>
            <textarea
              id="return-request-reason"
              name="returnRequestReason"
              [(ngModel)]="returnReason"
              rows="4"
              maxlength="1000"
              placeholder="Describe why you want to return this order. Include product condition, packaging condition, wrong item details, missing parts, or any delivery issue."></textarea>

            <div class="evidence-upload">
              <div>
                <strong>Evidence images</strong>
                <p>Upload clear photos of the defective item, wrong product, missing parts, damaged package, or delivery issue.</p>
              </div>
              <input #returnEvidenceInput type="file" accept="image/*" multiple hidden (change)="uploadReturnEvidence($event)">
              <button mat-stroked-button type="button" (click)="returnEvidenceInput.click()" [disabled]="uploadingReturnEvidence || returnEvidenceImageUrls.length >= 5">
                <mat-icon>add_photo_alternate</mat-icon>
                {{ uploadingReturnEvidence ? 'Uploading images...' : 'Upload evidence images' }}
              </button>
              <div class="evidence-grid" *ngIf="returnEvidenceImageUrls.length">
                <div class="evidence-thumb" *ngFor="let imageUrl of returnEvidenceImageUrls">
                  <img [src]="imageUrl" alt="Return evidence image">
                  <button mat-icon-button type="button" (click)="removeReturnEvidence(imageUrl)" aria-label="Remove evidence image">
                    <mat-icon>close</mat-icon>
                  </button>
                </div>
              </div>
            </div>

            <div class="return-request-actions">
              <button mat-button type="button" (click)="closeReturnRequestForm()" [disabled]="requestingReturn">Cancel</button>
              <button mat-raised-button color="primary" type="submit" [disabled]="!returnReason.trim() || requestingReturn">
                {{ requestingReturn ? 'Submitting return request...' : 'Submit return request' }}
              </button>
            </div>
          </form>

          <div class="info-grid">
            <div class="info-item">
              <mat-icon class="info-icon">location_on</mat-icon>
              <div>
                <span class="info-label">Shipping Address</span>
                <p class="info-val">{{ order.shippingAddress || 'N/A' }}</p>
              </div>
            </div>

            <div class="info-item">
              <mat-icon class="info-icon">note</mat-icon>
              <div>
                <span class="info-label">Order Notes</span>
                <p class="info-val">{{ order.note || 'No notes attached' }}</p>
              </div>
            </div>

            <div class="info-item">
              <mat-icon class="info-icon">local_shipping</mat-icon>
              <div>
                <span class="info-label">Shipping Fee</span>
                <p class="info-val">
                  <span *ngIf="!order.shippingFee || order.shippingFee === 0" class="free-shipping-tag">FREE</span>
                  <span *ngIf="order.shippingFee && order.shippingFee > 0">{{ order.shippingFee | currency:'USD':'symbol':'1.2-2' }}</span>
                </p>
              </div>
            </div>

            <div class="info-item">
              <mat-icon class="info-icon">payments</mat-icon>
              <div>
                <span class="info-label">Total Amount</span>
                <p class="info-val total-price text-gradient-cyan">{{ order.totalAmount | currency:'USD':'symbol':'1.2-2' }}</p>
              </div>
            </div>
          </div>
        </div>

        <div *ngIf="deliveryLoading" class="delivery-loading glass-panel" aria-live="polite">
          <mat-spinner diameter="30"></mat-spinner>
          <span>Loading delivery tracking...</span>
        </div>

        <app-delivery-timeline *ngIf="!deliveryLoading && delivery" [delivery]="delivery" />

        <div *ngIf="!deliveryLoading && deliveryError" class="delivery-empty glass-panel" role="status">
          <mat-icon>local_shipping</mat-icon>
          <div>
            <strong>Delivery tracking is not available yet</strong>
            <p>{{ deliveryError }}</p>
          </div>
          <button mat-stroked-button type="button" (click)="retryDelivery()">Retry</button>
        </div>

        <!-- Items Table Card -->
        <div class="order-items-card glass-panel">
          <h2 class="section-title text-gradient-purple">Purchased Items</h2>

          <table mat-table [dataSource]="order.items" class="full-width">
            <ng-container matColumnDef="productName">
              <th mat-header-cell *matHeaderCellDef>Product</th>
              <td mat-cell *matCellDef="let item">
                <div class="product-cell">
                  <mat-icon class="item-icon">inventory_2</mat-icon>
                  <div>
                    <strong class="item-name">{{ item.productName }}</strong>
                  </div>
                </div>
              </td>
            </ng-container>

            <ng-container matColumnDef="variantInfo">
              <th mat-header-cell *matHeaderCellDef>Variant & SKU</th>
              <td mat-cell *matCellDef="let item">
                <div class="variant-info-stack">
                  <span *ngIf="item.variantName" class="variant-name-badge">{{ item.variantName }}</span>
                  <span *ngIf="item.sku" class="sku-code">SKU: {{ item.sku }}</span>
                </div>
              </td>
            </ng-container>

            <ng-container matColumnDef="unitPrice">
              <th mat-header-cell *matHeaderCellDef>Unit Price</th>
              <td mat-cell *matCellDef="let item">
                {{ item.unitPrice | currency:'USD':'symbol':'1.2-2' }}
              </td>
            </ng-container>

            <ng-container matColumnDef="quantity">
              <th mat-header-cell *matHeaderCellDef>Quantity</th>
              <td mat-cell *matCellDef="let item" class="qty-cell">
                x{{ item.quantity }}
              </td>
            </ng-container>

            <ng-container matColumnDef="subtotal">
              <th mat-header-cell *matHeaderCellDef>Subtotal</th>
              <td mat-cell *matCellDef="let item" class="subtotal-cell text-gradient-cyan">
                {{ item.subtotal | currency:'USD':'symbol':'1.2-2' }}
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .review-btn {
      color: #0284c7 !important;
      border-color: rgba(2, 132, 199, 0.4) !important;
      font-size: 0.8rem;
      font-weight: 700;
    }
    .review-btn mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }
    .order-detail-container {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 20px;
      max-width: 1000px;
      margin: 0 auto;
    }
    .back-btn {
      color: var(--text-muted);
    }
    .loading-container {
      display: flex;
      justify-content: center;
      padding: 60px;
    }
    .order-content {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }
    .order-summary-header {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
    .title-status-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .order-title {
      font-size: 1.8rem;
      font-weight: 800;
      margin: 0;
    }
    .order-date {
      margin: 4px 0 0 0;
      color: var(--text-muted);
      font-size: 0.9rem;
    }
    .badge-pill {
      padding: 6px 14px;
      border-radius: 16px;
      font-size: 0.85rem;
      font-weight: 700;
      text-transform: uppercase;
    }
    .badge-pending { background: rgba(234, 179, 8, 0.2); color: #facc15; border: 1px solid rgba(234, 179, 8, 0.4); }
    .badge-confirmed { background: rgba(59, 130, 246, 0.2); color: #60a5fa; border: 1px solid rgba(59, 130, 246, 0.4); }
    .badge-processing { background: rgba(168, 85, 247, 0.2); color: #c084fc; border: 1px solid rgba(168, 85, 247, 0.4); }
    .badge-shipped { background: rgba(20, 184, 166, 0.2); color: #2dd4bf; border: 1px solid rgba(20, 184, 166, 0.4); }
    .badge-delivered { background: rgba(34, 197, 94, 0.2); color: #4ade80; border: 1px solid rgba(34, 197, 94, 0.4); }
    .badge-cancelled { background: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.4); }
    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 20px;
      padding-top: 16px;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }
    .info-item {
      display: flex;
      gap: 12px;
      align-items: flex-start;
    }
    .info-icon {
      color: var(--accent-cyan);
      margin-top: 2px;
    }
    .info-label {
      font-size: 0.8rem;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .info-val {
      margin: 4px 0 0 0;
      font-weight: 600;
    }
    .free-shipping-tag {
      color: #10b981;
      font-weight: 800;
      background: #ecfdf5;
      padding: 2px 8px;
      border-radius: 6px;
      border: 1px solid rgba(16, 185, 129, 0.3);
      font-size: 0.8rem;
    }
    .total-price {
      font-size: 1.2rem;
      font-weight: 800;
    }
    .order-items-card {
      padding: 24px;
    }
    .section-title {
      margin: 0 0 16px 0;
      font-size: 1.3rem;
      font-weight: 700;
    }
    .full-width {
      width: 100%;
    }
    .product-cell {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .item-icon {
      color: var(--accent-cyan);
    }
    .item-name {
      display: block;
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--text-main);
    }
    .variant-info-stack {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 4px 0;
    }
    .variant-name-badge {
      display: inline-block;
      font-size: 0.78rem;
      font-weight: 700;
      background: #eef2ff;
      color: #4f46e5;
      padding: 2px 8px;
      border-radius: 6px;
      border: 1px solid rgba(99, 102, 241, 0.2);
      width: fit-content;
    }
    .sku-id-row {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.75rem;
    }
    .sku-code {
      font-family: monospace;
      font-weight: 700;
      color: #334155;
      background: #f1f5f9;
      padding: 1px 6px;
      border-radius: 4px;
    }
    .variant-id-tag {
      color: var(--text-muted);
    }
    .qty-cell {
      font-weight: 700;
    }
    .subtotal-cell {
      font-weight: 800;
    }
    .delivery-loading, .delivery-empty {
      padding: 20px 24px;
      display: flex;
      align-items: center;
      gap: 14px;
    }
    .delivery-empty > mat-icon { color: var(--accent-cyan); }
    .delivery-empty div { flex: 1; }
    .delivery-empty p { margin: 4px 0 0; color: var(--text-muted); }
    .return-request-form {
      display: flex;
      flex-direction: column;
      gap: 10px;
      padding: 14px;
      border: 1px solid var(--border-subtle);
      border-radius: 8px;
      background: #f8fafc;
    }
    .return-request-form label {
      font-size: 0.82rem;
      font-weight: 800;
      color: #334155;
    }
    .return-request-form textarea {
      width: 100%;
      min-height: 104px;
      resize: vertical;
      border: 1px solid #cbd5e1;
      border-radius: 6px;
      padding: 10px;
      font: inherit;
      box-sizing: border-box;
    }
    .return-request-form select,
    .return-request-form input {
      width: 100%;
      border: 1px solid #cbd5e1;
      border-radius: 6px;
      padding: 10px;
      font: inherit;
      box-sizing: border-box;
      background: #ffffff;
    }
    .return-quantity-summary {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 11px 12px;
      border: 1px solid #dbe4ee;
      border-radius: 8px;
      background: #ffffff;
      color: #334155;
    }
    .return-quantity-summary strong {
      font-size: 0.84rem;
    }
    .return-quantity-summary span {
      font-weight: 800;
      color: #0f172a;
      text-align: right;
    }
    .return-request-actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
    }
    .evidence-upload {
      display: flex;
      flex-direction: column;
      gap: 10px;
      padding: 12px;
      border: 1px dashed #94a3b8;
      border-radius: 8px;
      background: #ffffff;
    }
    .evidence-upload p {
      margin: 4px 0 0;
      color: var(--text-muted);
      font-size: 0.86rem;
    }
    .evidence-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(84px, 1fr));
      gap: 10px;
    }
    .evidence-thumb {
      position: relative;
      aspect-ratio: 1;
      border-radius: 8px;
      overflow: hidden;
      border: 1px solid #cbd5e1;
      background: #f1f5f9;
    }
    .evidence-thumb img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .evidence-thumb button {
      position: absolute;
      top: 2px;
      right: 2px;
      width: 26px;
      height: 26px;
      background: rgba(15, 23, 42, 0.72);
      color: #ffffff;
    }
  `]
})
export class OrderDetailComponent implements OnInit {
  order: Order | null = null;
  delivery: Delivery | null = null;
  loading = true;
  deliveryLoading = false;
  deliveryError = '';
  confirming = false;
  cancelling = false;
  requestingReturn = false;
  showReturnRequestForm = false;
  returnOrderItemId: number | null = null;
  returnQuantity = 1;
  returnReason = '';
  returnEvidenceImageUrls: string[] = [];
  uploadingReturnEvidence = false;
  displayedColumns = ['productName', 'variantInfo', 'unitPrice', 'quantity', 'subtotal'];

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService,
    private reviewService: ReviewService,
    private deliveryService: DeliveryService,
    private notification: NotificationService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const orderId = Number(this.route.snapshot.paramMap.get('id'));
    if (orderId) {
      this.loadOrderDetail(orderId);
    }
  }

  loadOrderDetail(id: number): void {
    this.loading = true;
    this.delivery = null;
    this.deliveryError = '';
    this.orderService.getUserOrderById(id).pipe(
      tap((res) => {
        if (res.success && res.data) {
          this.order = res.data;
        }
      }),
      switchMap((res) => {
        if (!res.data || (res.data.status !== 'SHIPPED' && res.data.status !== 'DELIVERED')) {
          return of(null);
        }
        this.deliveryLoading = true;
        return this.deliveryService.getCustomerDelivery(id).pipe(
          catchError(() => {
            this.deliveryError = 'The warehouse has not published tracking details for this order.';
            return of(null);
          }),
          finalize(() => this.deliveryLoading = false)
        );
      }),
      finalize(() => this.loading = false)
    ).subscribe({
      next: (deliveryResponse) => {
        if (deliveryResponse?.success && deliveryResponse.data) {
          this.delivery = deliveryResponse.data;
        }
      },
      error: () => {
        this.notification.error('Failed to load order details');
      }
    });
  }

  retryDelivery(): void {
    if (!this.order) {
      return;
    }
    this.deliveryLoading = true;
    this.deliveryError = '';
    this.deliveryService.getCustomerDelivery(this.order.id).pipe(
      finalize(() => this.deliveryLoading = false)
    ).subscribe({
      next: (response) => {
        this.delivery = response.data ?? null;
      },
      error: () => {
        this.deliveryError = 'The warehouse has not published tracking details for this order.';
      }
    });
  }

  confirmReceived(): void {
    if (!this.order || this.confirming) {
      return;
    }
    const orderId = this.order.id;
    this.confirming = true;
    this.orderService.confirmReceived(orderId).pipe(
      finalize(() => this.confirming = false)
    ).subscribe({
      next: (res) => {
        if (res.success) {
          this.notification.success('Đã xác nhận nhận hàng. Bạn có thể đánh giá sản phẩm ngay bây giờ.');
          this.loadOrderDetail(orderId);
        }
      },
      error: (err) => {
        this.notification.error(err?.error?.message || 'Không thể xác nhận nhận hàng.');
      }
    });
  }

  cancelOrder(): void {
    if (!this.order || this.cancelling) {
      return;
    }
    const paidOnline = this.order.paymentStatus === 'PAID' && this.order.paymentMethod !== 'COD';
    const message = paidOnline
      ? `Order #${this.order.id} has been paid. Cancelling now will request a PayGate refund.`
      : `Cancel Order #${this.order.id}?`;
    const orderId = this.order.id;
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: paidOnline ? 'Cancel Order and Request PayGate Refund' : 'Cancel Order',
        message
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) {
        return;
      }
      this.cancelling = true;
      this.orderService.cancelOrder(orderId).pipe(
        finalize(() => this.cancelling = false)
      ).subscribe({
        next: () => {
          this.notification.success(paidOnline ? 'Order cancelled and refund requested' : 'Order cancelled');
          this.loadOrderDetail(orderId);
        },
        error: (err) => {
          this.notification.error(err?.error?.message || 'Failed to cancel order');
        }
      });
    });
  }

  openReturnRequestForm(): void {
    this.showReturnRequestForm = true;
    this.returnOrderItemId = null;
    this.returnQuantity = 1;
    this.returnReason = '';
    this.returnEvidenceImageUrls = [];
  }

  closeReturnRequestForm(): void {
    this.showReturnRequestForm = false;
    this.returnOrderItemId = null;
    this.returnQuantity = 1;
    this.returnReason = '';
    this.returnEvidenceImageUrls = [];
  }

  createReturnRequest(): void {
    if (!this.order || this.requestingReturn) {
      return;
    }
    const reason = this.returnReason.trim();
    if (!reason) {
      this.notification.error('Return request reason is required');
      return;
    }
    const item = this.returnOrderItemId
      ? this.refundableItems().find(candidate => candidate.id === this.returnOrderItemId)
      : undefined;
    const quantity = item ? this.refundableQuantity(item) : undefined;
    if (item && (!quantity || quantity <= 0 || quantity > this.refundableQuantity(item))) {
      this.notification.error(`Return quantity must be between 1 and ${this.refundableQuantity(item)}`);
      return;
    }
    const orderId = this.order.id;
    this.requestingReturn = true;
    this.orderService.createReturnRequest(orderId, {
      orderItemId: item?.id,
      quantity,
      reason,
      evidenceImageUrls: this.returnEvidenceImageUrls
    }).pipe(
      finalize(() => this.requestingReturn = false)
    ).subscribe({
      next: () => {
        this.notification.success('Return request created');
        this.closeReturnRequestForm();
        this.loadOrderDetail(orderId);
      },
      error: (err) => {
        this.notification.error(err?.error?.message || 'Failed to create return request');
      }
    });
  }

  canCancelOrder(): boolean {
    return !!this.order && ['PENDING', 'CONFIRMED', 'PROCESSING'].includes(this.order.status);
  }

  canRequestReturn(): boolean {
    return this.order?.status === 'SHIPPED' || this.order?.status === 'DELIVERED';
  }

  refundableItems(): Order['items'] {
    return (this.order?.items || []).filter(item => this.refundableQuantity(item) > 0);
  }

  refundableQuantity(item: Order['items'][number]): number {
    return item.quantity - (item.refundedQuantity || 0);
  }

  useMaxReturnQuantity(): void {
    const item = this.returnOrderItemId
      ? this.refundableItems().find(candidate => candidate.id === this.returnOrderItemId)
      : undefined;
    this.returnQuantity = item ? this.refundableQuantity(item) : 1;
  }

  returnQuantityLabel(): string {
    const item = this.returnOrderItemId
      ? this.refundableItems().find(candidate => candidate.id === this.returnOrderItemId)
      : undefined;
    if (item) {
      return `All ${this.refundableQuantity(item)} item(s)`;
    }
    const total = this.refundableItems().reduce((sum, current) => sum + this.refundableQuantity(current), 0);
    return `Entire order (${total} item(s))`;
  }

  uploadReturnEvidence(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []).slice(0, 5 - this.returnEvidenceImageUrls.length);
    input.value = '';
    if (files.length === 0) {
      return;
    }
    this.uploadingReturnEvidence = true;
    let completed = 0;
    const finishOne = () => {
      completed += 1;
      if (completed === files.length) {
        this.uploadingReturnEvidence = false;
      }
    };
    files.forEach(file => {
      this.reviewService.uploadImage(file).pipe(
        finalize(finishOne)
      ).subscribe({
        next: (response) => {
          if (response.success && response.data?.url) {
            this.returnEvidenceImageUrls = [...this.returnEvidenceImageUrls, response.data.url].slice(0, 5);
            this.notification.success('Evidence image uploaded');
          }
        },
        error: (err) => {
          this.notification.error(err?.error?.message || 'Failed to upload evidence image');
        }
      });
    });
  }

  removeReturnEvidence(imageUrl: string): void {
    this.returnEvidenceImageUrls = this.returnEvidenceImageUrls.filter(url => url !== imageUrl);
  }

  cancelButtonLabel(): string {
    if (this.order?.paymentStatus === 'PAID' && this.order.paymentMethod !== 'COD') {
      return 'Cancel & refund';
    }
    if (this.order?.paymentStatus === 'REFUND_PENDING') {
      return 'Refund pending';
    }
    return 'Cancel order';
  }

  canConfirmReceived(): boolean {
    if (this.order?.status !== 'SHIPPED') {
      return false;
    }
    if (this.deliveryLoading) {
      return false;
    }
    // No tracking record → allow (order shipped without delivery). With a record → only once in transit.
    return !this.delivery || this.delivery.status === 'IN_TRANSIT';
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'badge-pending';
      case 'CONFIRMED': return 'badge-confirmed';
      case 'PROCESSING': return 'badge-processing';
      case 'SHIPPED': return 'badge-shipped';
      case 'DELIVERED': return 'badge-delivered';
      case 'CANCELLED': return 'badge-cancelled';
      default: return 'badge-pending';
    }
  }
}
