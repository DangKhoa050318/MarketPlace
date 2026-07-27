import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';

import { AdminOrderService } from '../../../../core/services/admin-order.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { Order, OrderStatus } from '../../../../core/models/order.model';

@Component({
  selector: 'app-admin-order-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatSelectModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatMenuModule
  ],
  template: `
    <div class="admin-orders-container">
      <div class="admin-page-header">
        <div>
          <h1 class="page-title">
            <mat-icon class="title-icon">receipt_long</mat-icon> Admin Order Processing
          </h1>
          <p class="page-subtitle">Real-time order state transition control, tracking and fulfillment</p>
        </div>

        <button mat-icon-button class="refresh-btn" (click)="loadOrders()" matTooltip="Refresh orders list">
          <mat-icon>refresh</mat-icon>
        </button>
      </div>

      <div class="admin-card surface-card">
        <!-- Filter Controls Bar -->
        <div class="filter-bar">
          <mat-form-field appearance="outline" class="status-filter">
            <mat-label>Filter by Order Status</mat-label>
            <mat-select [(ngModel)]="selectedStatus" (selectionChange)="onStatusFilterChange()">
              <mat-option value="ALL">All Statuses (Show All)</mat-option>
              <mat-option value="PENDING">PENDING</mat-option>
              <mat-option value="CONFIRMED">CONFIRMED</mat-option>
              <mat-option value="PROCESSING">PROCESSING</mat-option>
              <mat-option value="SHIPPED">SHIPPED</mat-option>
              <mat-option value="DELIVERED">DELIVERED</mat-option>
              <mat-option value="CANCELLED">CANCELLED</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        <!-- Loading Spinner -->
        <div *ngIf="loading" class="spinner-container">
          <mat-spinner diameter="44"></mat-spinner>
        </div>

        <!-- Orders Table -->
        <div class="table-container" *ngIf="!loading">
          <table mat-table [dataSource]="orders" class="full-width">
            <!-- ID Column -->
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef> Order ID </th>
              <td mat-cell *matCellDef="let order" class="order-id-cell"> #{{ order.id }} </td>
            </ng-container>

            <!-- Customer Email Column -->
            <ng-container matColumnDef="userEmail">
              <th mat-header-cell *matHeaderCellDef> Customer </th>
              <td mat-cell *matCellDef="let order">
                <div class="customer-cell">
                  <span class="user-avatar-badge">{{ (order.username || order.userEmail || 'U')[0].toUpperCase() }}</span>
                  <div class="user-info-stack">
                    <strong class="user-name-title">{{ order.username || ('User #' + order.userId) }}</strong>
                    <small class="user-email-subtitle">{{ order.userEmail }}</small>
                  </div>
                </div>
              </td>
            </ng-container>

            <!-- Created At Column -->
            <ng-container matColumnDef="createdAt">
              <th mat-header-cell *matHeaderCellDef> Created At </th>
              <td mat-cell *matCellDef="let order" class="date-cell"> {{ order.createdAt | date:'medium' }} </td>
            </ng-container>

            <!-- Total Amount Column -->
            <ng-container matColumnDef="totalAmount">
              <th mat-header-cell *matHeaderCellDef> Total Amount </th>
              <td mat-cell *matCellDef="let order" class="amount-cell">
                \${{ order.totalAmount | number:'1.2-2' }}
              </td>
            </ng-container>

            <!-- Status Column -->
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef> Current Status </th>
              <td mat-cell *matCellDef="let order">
                <span class="badge-pill" [ngClass]="getStatusChipClass(order.status)">
                  {{ order.status }}
                </span>
              </td>
            </ng-container>

            <!-- Actions Column -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef> Action / Status Transition </th>
              <td mat-cell *matCellDef="let order">
                <div class="actions-cell-group">
                  <!-- Quick Status Transition Menu Dropdown -->
                  <button
                    mat-stroked-button
                    color="primary"
                    class="quick-status-btn"
                    [matMenuTriggerFor]="statusMenu"
                    [disabled]="order.status === 'DELIVERED' || order.status === 'CANCELLED'">
                    <span>Change Status</span>
                    <mat-icon>arrow_drop_down</mat-icon>
                  </button>

                  <mat-menu #statusMenu="matMenu">
                    <button
                      mat-menu-item
                      *ngFor="let nextStatus of getNextAllowedStatuses(order.status)"
                      (click)="quickUpdateStatus(order, nextStatus)">
                      <span class="badge-pill" [ngClass]="getStatusChipClass(nextStatus)">{{ nextStatus }}</span>
                    </button>
                    <div *ngIf="getNextAllowedStatuses(order.status).length === 0" class="no-status-item">
                      No transitions available
                    </div>
                  </mat-menu>

                  <!-- Open Modal for Details & Note -->
                  <button mat-icon-button class="edit-note-btn" (click)="openUpdateModal(order)" matTooltip="Add Note / Custom Details">
                    <mat-icon>edit_note</mat-icon>
                  </button>
                </div>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>

          <!-- Empty State -->
          <div *ngIf="orders.length === 0" class="empty-state">
            <mat-icon class="empty-icon">inbox</mat-icon>
            <p>No orders found matching the filter criteria.</p>
          </div>
        </div>

        <!-- Pagination -->
        <mat-paginator
          [length]="totalElements"
          [pageSize]="pageSize"
          [pageSizeOptions]="[10, 25, 50, 100]"
          (page)="onPageChange($event)"
          showFirstLastButtons>
        </mat-paginator>
      </div>

      <!-- Update Order Status Modal -->
      <div *ngIf="selectedOrder" class="update-modal-backdrop">
        <div class="update-card surface-card">
          <div class="modal-header">
            <h3>Update Order #{{ selectedOrder.id }} Status</h3>
            <button mat-icon-button (click)="closeUpdateModal()"><mat-icon>close</mat-icon></button>
          </div>

          <div class="modal-body">
            <div class="order-details-box">
              <p><strong>Customer:</strong> {{ selectedOrder.username || ('User #' + selectedOrder.userId) }} ({{ selectedOrder.userEmail }})</p>
              <p><strong>Shipping Address:</strong> {{ selectedOrder.shippingAddress || 'N/A' }}</p>
              <p><strong>Current Status:</strong> <span class="badge-pill" [ngClass]="getStatusChipClass(selectedOrder.status)">{{ selectedOrder.status }}</span></p>
            </div>

            <form [formGroup]="updateForm" (ngSubmit)="onSaveStatusUpdate()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>New Order Status</mat-label>
                <mat-select formControlName="status">
                  <mat-option *ngFor="let targetStatus of getNextAllowedStatuses(selectedOrder.status)" [value]="targetStatus">
                    {{ targetStatus }}
                  </mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Update Note (Optional)</mat-label>
                <textarea matInput formControlName="note" rows="2" placeholder="e.g. Shipped via Express Tracking #99482"></textarea>
              </mat-form-field>

              <div class="modal-actions">
                <button mat-button type="button" (click)="closeUpdateModal()">Cancel</button>
                <button mat-raised-button class="btn-solid-primary" type="submit" [disabled]="updateForm.invalid || updating">
                  <mat-spinner *ngIf="updating" diameter="20"></mat-spinner>
                  <span *ngIf="!updating">Save Changes</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-orders-container {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .admin-page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .page-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0;
      font-size: 2rem;
      font-weight: 800;
      color: var(--text-main);
    }

    .title-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: #4f46e5;
    }

    .page-subtitle {
      margin: 4px 0 0 0;
      color: var(--text-muted);
    }

    .refresh-btn {
      color: var(--primary);
    }

    .admin-card {
      padding: 20px;
    }

    .filter-bar {
      margin-bottom: 16px;
    }

    .status-filter {
      min-width: 280px;
    }

    .spinner-container {
      display: flex;
      justify-content: center;
      padding: 48px;
    }

    .table-container {
      overflow-x: auto;
    }

    .order-id-cell {
      font-weight: 800;
      color: #4f46e5;
    }

    .customer-cell {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .user-avatar-badge {
      width: 32px;
      height: 32px;
      border-radius: 8px;
      background: var(--primary-subtle);
      color: var(--primary);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      font-size: 0.85rem;
      flex-shrink: 0;
    }

    .user-info-stack {
      display: flex;
      flex-direction: column;
      line-height: 1.2;
    }

    .user-name-title {
      color: var(--text-main);
      font-size: 0.9rem;
      font-weight: 700;
    }

    .user-email-subtitle {
      color: var(--text-muted);
      font-size: 0.78rem;
    }

    .date-cell {
      color: var(--text-muted);
      font-size: 0.85rem;
    }

    .amount-cell {
      font-weight: 800;
      font-size: 1.05rem;
      color: var(--text-main);
    }

    .actions-cell-group {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .quick-status-btn {
      border-radius: 8px;
      font-size: 0.8rem;
      font-weight: 700;
    }

    .edit-note-btn {
      color: #4f46e5;
    }

    .no-status-item {
      padding: 8px 16px;
      font-size: 0.82rem;
      color: var(--text-muted);
    }

    .empty-state {
      text-align: center;
      padding: 48px;
      color: var(--text-muted);
    }

    .empty-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #0284c7;
    }

    /* Modal Backdrop */
    .update-modal-backdrop {
      position: fixed;
      top: 0;
      left: 0;
      width: 100vw;
      height: 100vh;
      background: rgba(15, 23, 42, 0.6);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .update-card {
      width: 460px;
      max-width: 90vw;
      padding: 24px;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border-subtle);
      padding-bottom: 12px;
      margin-bottom: 16px;
    }

    .modal-header h3 {
      margin: 0;
      font-weight: 700;
      color: var(--text-main);
    }

    .order-details-box {
      background: #f8fafc;
      padding: 12px 16px;
      border-radius: 10px;
      border: 1px solid var(--border-subtle);
      margin-bottom: 16px;
      font-size: 0.9rem;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 16px;
    }
  `]
})
export class AdminOrderListComponent implements OnInit {
  displayedColumns: string[] = ['id', 'userEmail', 'createdAt', 'totalAmount', 'status', 'actions'];
  orders: Order[] = [];
  selectedStatus: string = 'ALL';
  loading = false;
  updating = false;

  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;

  selectedOrder: Order | null = null;
  updateForm: FormGroup;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private adminOrderService: AdminOrderService,
    private notificationService: NotificationService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.updateForm = this.fb.group({
      status: ['', Validators.required],
      note: ['']
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['status']) {
        this.selectedStatus = params['status'].toUpperCase();
      }
      this.loadOrders();
    });
  }

  loadOrders(): void {
    this.loading = true;
    this.adminOrderService.getAdminOrders(this.selectedStatus, this.pageIndex, this.pageSize)
      .subscribe({
        next: (response) => {
          this.loading = false;
          if (response.success && response.data) {
            this.orders = response.data.content;
            this.totalElements = response.data.totalElements;
          }
        },
        error: (err) => {
          this.loading = false;
          this.notificationService.error(err.error?.message || 'Failed to load admin orders');
        }
      });
  }

  onStatusFilterChange(): void {
    this.pageIndex = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { status: this.selectedStatus === 'ALL' ? null : this.selectedStatus },
      queryParamsHandling: 'merge'
    });
    this.loadOrders();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadOrders();
  }

  getStatusChipClass(status: OrderStatus): string {
    switch (status) {
      case 'PENDING': return 'badge-pending';
      case 'CONFIRMED': return 'badge-confirmed';
      case 'PROCESSING': return 'badge-processing';
      case 'SHIPPED': return 'badge-shipped';
      case 'DELIVERED': return 'badge-delivered';
      case 'CANCELLED': return 'badge-pending';
      default: return 'badge-confirmed';
    }
  }

  getNextAllowedStatuses(currentStatus: OrderStatus): OrderStatus[] {
    switch (currentStatus) {
      case 'PENDING': return ['CONFIRMED', 'CANCELLED'];
      case 'CONFIRMED': return ['PROCESSING', 'CANCELLED'];
      case 'PROCESSING': return ['SHIPPED', 'CANCELLED'];
      case 'SHIPPED': return ['DELIVERED'];
      default: return [];
    }
  }

  quickUpdateStatus(order: Order, newStatus: OrderStatus): void {
    this.adminOrderService.updateOrderStatus(order.id, newStatus, order.note)
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.notificationService.success(`Order #${order.id} status updated to ${newStatus}`);
            this.loadOrders();
          }
        },
        error: (err) => {
          this.notificationService.error(err.error?.message || 'Failed to update order status');
        }
      });
  }

  openUpdateModal(order: Order): void {
    this.selectedOrder = order;
    const allowed = this.getNextAllowedStatuses(order.status);
    this.updateForm.patchValue({
      status: allowed.length > 0 ? allowed[0] : order.status,
      note: order.note || ''
    });
  }

  closeUpdateModal(): void {
    this.selectedOrder = null;
    this.updateForm.reset();
  }

  onSaveStatusUpdate(): void {
    if (this.updateForm.invalid || !this.selectedOrder) {
      return;
    }

    this.updating = true;
    const { status, note } = this.updateForm.value;

    this.adminOrderService.updateOrderStatus(this.selectedOrder.id, status, note)
      .subscribe({
        next: (response) => {
          this.updating = false;
          if (response.success) {
            this.notificationService.success(`Order #${this.selectedOrder?.id} status updated to ${status}`);
            this.closeUpdateModal();
            this.loadOrders();
          }
        },
        error: (err) => {
          this.updating = false;
          this.notificationService.error(err.error?.message || 'Failed to update order status');
        }
      });
  }
}
