import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { OrderService, Order } from '../../../core/services/order.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDialogModule
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

            <!-- Total Items -->
            <ng-container matColumnDef="totalItems">
              <th mat-header-cell *matHeaderCellDef>Items</th>
              <td mat-cell *matCellDef="let order">
                {{ order.items ? order.items.length : 0 }} items
              </td>
            </ng-container>

            <!-- Total Amount -->
            <ng-container matColumnDef="totalAmount">
              <th mat-header-cell *matHeaderCellDef>Total Amount</th>
              <td mat-cell *matCellDef="let order" class="total-amount text-gradient-cyan">
                {{ order.totalAmount | currency:'USD':'symbol':'1.2-2' }}
              </td>
            </ng-container>

            <!-- Status -->
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let order">
                <span class="badge-pill" [ngClass]="getStatusBadgeClass(order.status)">
                  {{ order.status }}
                </span>
              </td>
            </ng-container>

            <!-- Actions -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Actions</th>
              <td mat-cell *matCellDef="let order">
                <div class="action-buttons">
                  <a mat-icon-button color="primary" [routerLink]="['/orders', order.id]" title="View Order Details">
                    <mat-icon>visibility</mat-icon>
                  </a>
                  <button 
                    *ngIf="order.status === 'PENDING' || order.status === 'CONFIRMED'" 
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
            [pageSizeOptions]="[5, 10, 20]"
            (page)="onPageChange($event)"
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
    .glass-paginator {
      background: transparent !important;
      color: var(--text-main);
    }
  `]
})
export class OrderListComponent implements OnInit {
  orders: Order[] = [];
  loading = true;
  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  displayedColumns = ['id', 'createdAt', 'totalItems', 'totalAmount', 'status', 'actions'];

  constructor(
    private orderService: OrderService,
    private notification: NotificationService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.orderService.getUserOrders(this.currentPage, this.pageSize).subscribe({
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
