import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { OrderService, Order } from '../../../core/services/order.service';
import { ReviewService } from '../../../core/services/review.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { OrderReviewDialogComponent } from '../order-review-dialog/order-review-dialog.component';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    OrderReviewDialogComponent
  ],
  template: `
    <div class="orders-page-container">
      <div class="orders-header">
        <div>
          <h1 class="page-title text-gradient-cyan">
            <mat-icon class="title-icon">receipt_long</mat-icon> My Order History
          </h1>
          <p class="page-subtitle">Track your placed orders, delivery status, and payment receipts</p>
        </div>
      </div>

      <!-- Filter Controls Bar -->
      <div class="glass-panel filter-bar">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search Orders & Products</mat-label>
          <input matInput [(ngModel)]="searchQuery" (keyup.enter)="applyFilters()" placeholder="Order ID or product name...">
          <button *ngIf="searchQuery" matSuffix mat-icon-button (click)="searchQuery=''; applyFilters()">
            <mat-icon>close</mat-icon>
          </button>
          <mat-icon matSuffix *ngIf="!searchQuery">search</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="outline" class="filter-select">
          <mat-label>Order Status</mat-label>
          <mat-select [(ngModel)]="selectedStatus" (selectionChange)="applyFilters()">
            <mat-option value="">All</mat-option>
            <mat-option value="PENDING">Pending</mat-option>
            <mat-option value="CONFIRMED">Confirmed</mat-option>
            <mat-option value="PROCESSING">Processing</mat-option>
            <mat-option value="SHIPPED">Shipped</mat-option>
            <mat-option value="DELIVERED">Delivered</mat-option>
            <mat-option value="CANCELLED">Cancelled</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="filter-select">
          <mat-label>Payment Status</mat-label>
          <mat-select [(ngModel)]="selectedPaymentStatus" (selectionChange)="applyFilters()">
            <mat-option value="">All</mat-option>
            <mat-option value="UNPAID">Unpaid</mat-option>
            <mat-option value="PENDING_PAYGATE">Pending PayGate</mat-option>
            <mat-option value="PAID">Paid</mat-option>
          </mat-select>
        </mat-form-field>

        <button mat-stroked-button class="btn-clear-filter" (click)="resetFilters()" *ngIf="hasActiveFilters()">
          <mat-icon>filter_alt_off</mat-icon> Reset Filters
        </button>
      </div>

      <div *ngIf="loading" class="loading-container">
        <mat-spinner diameter="44"></mat-spinner>
      </div>

      <div *ngIf="!loading">
        <div *ngIf="orders.length === 0" class="empty-orders glass-panel">
          <div class="empty-icon-circle">
            <mat-icon class="empty-icon">shopping_bag</mat-icon>
          </div>
          <h2>No Orders Placed Yet</h2>
          <p>Explore our storefront catalog and place your first order today.</p>
          <a mat-raised-button class="btn-glowing" routerLink="/products">
            <mat-icon>storefront</mat-icon> Browse Products
          </a>
        </div>

        <div *ngIf="orders.length > 0" class="glass-panel table-container">
          <table mat-table [dataSource]="orders" class="full-width">
            <!-- Order ID -->
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef>Order ID</th>
              <td mat-cell *matCellDef="let order">
                <span class="order-id-badge">#{{ order.id }}</span>
              </td>
            </ng-container>

            <!-- Date -->
            <ng-container matColumnDef="createdAt">
              <th mat-header-cell *matHeaderCellDef>Order Date</th>
              <td mat-cell *matCellDef="let order">
                {{ order.createdAt | date:'medium' }}
              </td>
            </ng-container>

            <!-- Total Items & Variant Preview -->
            <ng-container matColumnDef="itemsPreview">
              <th mat-header-cell *matHeaderCellDef>Purchased Items & Variants</th>
              <td mat-cell *matCellDef="let order">
                <div class="items-preview-stack">
                  <div *ngFor="let item of (order.items || []).slice(0, 2)" class="variant-item-pill">
                    <span class="item-product-name">{{ item.productName }}</span>
                    <span *ngIf="item.variantName" class="item-variant-chip">{{ item.variantName }}</span>
                    <span *ngIf="!item.variantName && item.sku" class="item-variant-chip">{{ item.sku }}</span>
                    <span class="item-qty">x{{ item.quantity }}</span>
                  </div>
                  <div *ngIf="order.items && order.items.length > 2" class="more-items-badge">
                    +{{ order.items.length - 2 }} more item(s)
                  </div>
                </div>
              </td>
            </ng-container>

            <!-- Total Amount -->
            <ng-container matColumnDef="totalAmount">
              <th mat-header-cell *matHeaderCellDef>Total Amount</th>
              <td mat-cell *matCellDef="let order" class="total-amount text-gradient-cyan">
                {{ order.totalAmount | currency:'USD':'symbol':'1.2-2' }}
              </td>
            </ng-container>

            <!-- Order Status -->
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Order Status</th>
              <td mat-cell *matCellDef="let order">
                <span class="badge-pill" [ngClass]="getStatusBadgeClass(order.status)">
                  {{ order.status }}
                </span>
              </td>
            </ng-container>

            <!-- Payment Method -->
            <ng-container matColumnDef="paymentMethod">
              <th mat-header-cell *matHeaderCellDef>Payment</th>
              <td mat-cell *matCellDef="let order">
                <span *ngIf="order.paymentMethod" class="badge-payment-chip">
                  <span class="pay-method-text">{{ order.paymentMethod === 'COD' ? 'COD' : (order.paymentMethod === 'PAYGATE_BNPL' ? 'BNPL' : 'Paygate Card') }}</span>
                </span>
                <span *ngIf="!order.paymentMethod" class="text-muted">—</span>
              </td>
            </ng-container>

            <!-- Actions -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Actions</th>
              <td mat-cell *matCellDef="let order">
                <div class="action-buttons">
                  <button
                    *ngIf="isPaymentEligibleForRetry(order)"
                    mat-raised-button
                    class="btn-pay-again"
                    (click)="onRetryPayment(order)"
                    [disabled]="retryingOrderId === order.id"
                    title="Tiếp tục thanh toán qua PayGate">
                    <mat-icon>payment</mat-icon>
                    <span>{{ retryingOrderId === order.id ? 'Đang chuyển...' : 'Tiếp tục thanh toán' }}</span>
                  </button>
                  <button
                    *ngIf="order.status === 'SHIPPED'"
                    mat-raised-button
                    class="btn-confirm-received"
                    (click)="onConfirmReceived(order)"
                    title="Xác nhận đã nhận hàng">
                    <mat-icon>check_circle</mat-icon>
                    <span>Đã nhận hàng</span>
                  </button>
                  <button
                    *ngIf="hasUnreviewedItems(order)"
                    mat-stroked-button
                    class="btn-review-order"
                    (click)="openOrderReviewModal(order)"
                    title="Review Purchased Items">
                    <mat-icon>rate_review</mat-icon>
                    <span>Đánh giá</span>
                  </button>
                  <a mat-icon-button color="primary" [routerLink]="['/orders', order.id]" title="View Order Details">
                    <mat-icon>visibility</mat-icon>
                  </a>
                  <button 
                    *ngIf="isCancelEligible(order)" 
                    mat-icon-button 
                    color="warn" 
                    (click)="onCancelOrder(order)" 
                    title="Cancel Order">
                    <mat-icon>cancel</mat-icon>
                  </button>
                </div>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>

          <mat-paginator
            [length]="totalElements"
            [pageSize]="pageSize"
            [pageIndex]="currentPage"
            [pageSizeOptions]="[5, 10, 20]"
            (page)="onPageChange($event)"
            showFirstLastButtons
            class="glass-paginator">
          </mat-paginator>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .orders-page-container {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 24px;
      max-width: 1200px;
      margin: 0 auto;
    }
    .orders-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .page-title {
      font-size: 2rem;
      font-weight: 800;
      margin: 0;
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .title-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: var(--accent-cyan);
    }
    .page-subtitle {
      margin: 4px 0 0 0;
      color: var(--text-muted);
      font-size: 0.95rem;
    }
    .loading-container {
      display: flex;
      justify-content: center;
      padding: 60px;
    }
    .empty-orders {
      padding: 60px 24px;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
    }
    .empty-icon-circle {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: rgba(0, 242, 254, 0.1);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .empty-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: var(--accent-cyan);
    }
    .table-container {
      overflow: hidden;
    }
    .full-width {
      width: 100%;
    }
    .order-id-badge {
      font-weight: 700;
      font-family: monospace;
      color: var(--accent-cyan);
    }
    .total-amount {
      font-weight: 800;
      font-size: 1.05rem;
    }
    .badge-pill {
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
    }
    .badge-pending { background: rgba(234, 179, 8, 0.2); color: #facc15; border: 1px solid rgba(234, 179, 8, 0.4); }
    .badge-confirmed { background: rgba(59, 130, 246, 0.2); color: #60a5fa; border: 1px solid rgba(59, 130, 246, 0.4); }
    .badge-processing { background: rgba(168, 85, 247, 0.2); color: #c084fc; border: 1px solid rgba(168, 85, 247, 0.4); }
    .badge-shipped { background: rgba(20, 184, 166, 0.2); color: #2dd4bf; border: 1px solid rgba(20, 184, 166, 0.4); }
    .badge-delivered { background: rgba(34, 197, 94, 0.2); color: #4ade80; border: 1px solid rgba(34, 197, 94, 0.4); }
    .badge-cancelled { background: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.4); }
    .action-buttons {
      display: flex;
      gap: 4px;
    }
    .items-preview-stack {
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 6px 0;
      width: 100%;
      min-width: 320px;
    }
    .variant-item-pill {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.83rem;
      background: #f8fafc;
      border: 1px solid var(--border-subtle, #e2e8f0);
      padding: 5px 10px;
      border-radius: 8px;
      width: 100%;
      max-width: 380px;
      box-sizing: border-box;
    }
    .item-product-name {
      flex: 1;
      min-width: 0;
      font-weight: 650;
      color: #0f172a;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .item-variant-chip {
      flex-shrink: 0;
      font-size: 0.72rem;
      font-weight: 700;
      background: #eef2ff;
      color: #4f46e5;
      padding: 2px 7px;
      border-radius: 4px;
      border: 1px solid rgba(99, 102, 241, 0.2);
      white-space: nowrap;
    }
    .item-qty {
      flex-shrink: 0;
      font-size: 0.75rem;
      font-weight: 800;
      color: #0284c7;
      background: #e0f2fe;
      padding: 2px 6px;
      border-radius: 4px;
      white-space: nowrap;
    }
    .status-payment-col {
      display: flex;
      flex-direction: column;
      gap: 5px;
      align-items: center;
      justify-content: center;
      width: 100%;
    }
    .badge-payment-chip {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 0.68rem;
      font-weight: 750;
      background: #f8fafc;
      color: #334155;
      padding: 2px 8px;
      border-radius: 10px;
      border: 1px solid #e2e8f0;
      text-transform: uppercase;
      letter-spacing: 0.03em;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
    }
    .pay-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      display: inline-block;
    }
    .pay-dot.dot-paid {
      background-color: #10b981;
      box-shadow: 0 0 4px rgba(16, 185, 129, 0.6);
    }
    .pay-dot.dot-unpaid {
      background-color: #94a3b8;
    }
    .more-items-badge {
      font-size: 0.75rem;
      font-weight: 650;
      color: var(--text-muted);
      margin-left: 2px;
    }
    .action-buttons {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      vertical-align: middle;
      white-space: nowrap;
    }
    .btn-pay-again {
      background: linear-gradient(135deg, #0284c7 0%, #2563eb 100%) !important;
      color: #ffffff !important;
      font-size: 0.78rem !important;
      font-weight: 700 !important;
      height: 34px !important;
      padding: 0 12px !important;
      border-radius: 8px !important;
      box-shadow: 0 3px 10px rgba(2, 132, 199, 0.35) !important;
      white-space: nowrap !important;
    }
    .btn-pay-again ::ng-deep .mdc-button__label,
    .btn-pay-again .mdc-button__label {
      display: inline-flex !important;
      align-items: center !important;
      gap: 5px !important;
      white-space: nowrap !important;
      line-height: 1 !important;
    }
    .btn-pay-again mat-icon {
      font-size: 17px !important;
      width: 17px !important;
      height: 17px !important;
      margin: 0 !important;
    }
    .btn-confirm-received {
      background: linear-gradient(135deg, #059669 0%, #10b981 100%) !important;
      color: #ffffff !important;
      font-size: 0.78rem !important;
      font-weight: 700 !important;
      height: 34px !important;
      padding: 0 12px !important;
      border-radius: 8px !important;
      box-shadow: 0 3px 10px rgba(16, 185, 129, 0.35) !important;
      white-space: nowrap !important;
    }
    .btn-confirm-received ::ng-deep .mdc-button__label,
    .btn-confirm-received .mdc-button__label {
      display: inline-flex !important;
      align-items: center !important;
      gap: 5px !important;
      white-space: nowrap !important;
      line-height: 1 !important;
    }
    .btn-confirm-received mat-icon {
      font-size: 17px !important;
      width: 17px !important;
      height: 17px !important;
      margin: 0 !important;
    }
    .btn-review-order {
      color: #0284c7 !important;
      border-color: rgba(2, 132, 199, 0.4) !important;
      font-size: 0.78rem !important;
      font-weight: 700 !important;
      height: 34px !important;
      padding: 0 10px !important;
      border-radius: 8px !important;
      white-space: nowrap !important;
    }
    .btn-review-order ::ng-deep .mdc-button__label,
    .btn-review-order .mdc-button__label {
      display: inline-flex !important;
      align-items: center !important;
      gap: 4px !important;
      white-space: nowrap !important;
      line-height: 1 !important;
    }
    .btn-review-order mat-icon {
      font-size: 16px !important;
      width: 16px !important;
      height: 16px !important;
      margin: 0 !important;
    }
    .glass-paginator {
      background: transparent !important;
      color: var(--text-main);
    }

    .filter-bar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 16px;
      padding: 16px 20px;
      margin-bottom: 20px;
      background: rgba(255, 255, 255, 0.03);
      border-radius: 12px;
      border: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
    }
    .search-field {
      flex: 1 1 280px;
    }
    .filter-select {
      flex: 0 1 200px;
    }
    .btn-clear-filter {
      height: 48px !important;
      margin-bottom: 22px;
      border-color: rgba(255, 255, 255, 0.2) !important;
      color: var(--text-main) !important;
    }
  `]
})
export class OrderListComponent implements OnInit {
  orders: Order[] = [];
  loading = true;
  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  displayedColumns = ['id', 'createdAt', 'itemsPreview', 'totalAmount', 'paymentMethod', 'status', 'actions'];
  reviewedOrderItemIds = new Set<number>();

  constructor(
    private orderService: OrderService,
    private reviewService: ReviewService,
    private notification: NotificationService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadOrders();
    this.loadReviewedOrderItemIds();
  }

  loadReviewedOrderItemIds(): void {
    this.reviewService.getMyReviewedOrderItemIds().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.reviewedOrderItemIds = new Set(res.data);
        }
      },
      error: () => {
        // Silently handle unauthenticated or non-customer status
      }
    });
  }

  hasUnreviewedItems(order: Order): boolean {
    if (order.status !== 'DELIVERED') return false;
    if (!order.items || order.items.length === 0) return false;
    return order.items.some(item => !this.reviewedOrderItemIds.has(item.id));
  }

  openOrderReviewModal(order: Order): void {
    const dialogRef = this.dialog.open(OrderReviewDialogComponent, {
      width: '620px',
      data: {
        order,
        reviewedOrderItemIds: this.reviewedOrderItemIds
      }
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res?.reviewedOrderItemIds) {
        this.reviewedOrderItemIds = new Set(res.reviewedOrderItemIds);
      }
    });
  }

  searchQuery = '';
  selectedStatus = '';
  selectedPaymentStatus = '';

  hasActiveFilters(): boolean {
    return !!(this.searchQuery.trim() || this.selectedStatus || this.selectedPaymentStatus);
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadOrders();
  }

  resetFilters(): void {
    this.searchQuery = '';
    this.selectedStatus = '';
    this.selectedPaymentStatus = '';
    this.currentPage = 0;
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.orderService.getUserOrders(
      this.currentPage,
      this.pageSize,
      this.selectedStatus || undefined,
      this.selectedPaymentStatus || undefined,
      this.searchQuery || undefined
    ).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.data) {
          this.orders = res.data.content;
          this.totalElements = res.data.totalElements;
        }
      },
      error: () => {
        this.loading = false;
        this.notification.error('Failed to load order history');
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadOrders();
  }

  onCancelOrder(order: Order): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancel Order',
        message: `Are you sure you want to cancel Order #${order.id}?`
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.orderService.cancelOrder(order.id).subscribe({
          next: () => {
            this.notification.success(`Order #${order.id} cancelled successfully`);
            this.loadOrders();
          },
          error: (err) => {
            this.notification.error(err.error?.message || 'Failed to cancel order');
          }
        });
      }
    });
  }

  retryingOrderId: number | null = null;

  isPaymentEligibleForRetry(order: Order): boolean {
    return order.paymentStatus !== 'PAID' &&
           order.status !== 'CANCELLED' &&
           order.status !== 'DELIVERED' &&
           order.paymentMethod !== 'COD';
  }

  isCancelEligible(order: Order): boolean {
    return (order.status === 'PENDING' || order.status === 'CONFIRMED') &&
           order.paymentStatus !== 'PAID';
  }

  onConfirmReceived(order: Order): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Xác nhận đã nhận hàng',
        message: `Bạn xác nhận đã nhận được đơn hàng #${order.id}?`
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.orderService.confirmReceived(order.id).subscribe({
          next: () => {
            this.notification.success(`Đã xác nhận nhận thành công đơn hàng #${order.id}`);
            this.loadOrders();
          },
          error: (err) => {
            this.notification.error(err.error?.message || 'Không thể cập nhật trạng thái đơn hàng');
          }
        });
      }
    });
  }

  onRetryPayment(order: Order): void {
    this.retryingOrderId = order.id;
    this.orderService.retryPayment(order.id).subscribe({
      next: (res) => {
        this.retryingOrderId = null;
        const targetUrl = res.data?.paymentUrl;
        if (targetUrl) {
          this.notification.info('Đang chuyển hướng sang cổng thanh toán PayGate...');
          window.location.href = targetUrl;
        } else {
          this.notification.error('Không thể khởi tạo phiên thanh toán mới. Vui lòng thử lại sau.');
        }
      },
      error: (err) => {
        this.retryingOrderId = null;
        this.notification.error(err.error?.message || 'Không thể kết nối đến hệ thống thanh toán.');
      }
    });
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
