import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrderService, Order } from '../../../core/services/order.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule
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

            <span class="badge-pill" [ngClass]="getStatusBadgeClass(order.status)">
              {{ order.status }}
            </span>
          </div>

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
              <mat-icon class="info-icon">payments</mat-icon>
              <div>
                <span class="info-label">Total Amount</span>
                <p class="info-val total-price text-gradient-cyan">{{ order.totalAmount | currency:'USD':'symbol':'1.2-2' }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Items Table Card -->
        <div class="order-items-card glass-panel">
          <h2 class="section-title text-gradient-purple">Purchased Items</h2>

          <table mat-table [dataSource]="order.items" class="full-width">
            <ng-container matColumnDef="productName">
              <th mat-header-cell *matHeaderCellDef>Product Name</th>
              <td mat-cell *matCellDef="let item">
                <div class="product-cell">
                  <mat-icon class="item-icon">inventory_2</mat-icon>
                  <div>
                    <strong class="item-name">{{ item.productName }}</strong>
                    <span class="item-pid">ID: #{{ item.productId }}</span>
                  </div>
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
    }
    .item-pid {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    .qty-cell {
      font-weight: 700;
    }
    .subtotal-cell {
      font-weight: 800;
    }
  `]
})
export class OrderDetailComponent implements OnInit {
  order: Order | null = null;
  loading = true;
  displayedColumns = ['productName', 'unitPrice', 'quantity', 'subtotal'];

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    const orderId = Number(this.route.snapshot.paramMap.get('id'));
    if (orderId) {
      this.loadOrderDetail(orderId);
    }
  }

  loadOrderDetail(id: number): void {
    this.loading = true;
    this.orderService.getUserOrderById(id).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.data) {
          this.order = res.data;
        }
      },
      error: () => {
        this.loading = false;
        this.notification.error('Failed to load order details');
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
