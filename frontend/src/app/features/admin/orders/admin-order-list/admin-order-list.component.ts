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
import { DeliveryService } from '../../../../core/services/delivery.service';
import { Delivery, DeliveryEventType, DeliveryStatus } from '../../../../core/models/delivery.model';
import { DeliveryTimelineComponent } from '../../../../shared/components/delivery-timeline/delivery-timeline.component';

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
    MatMenuModule,
    DeliveryTimelineComponent
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
                    [disabled]="getNextAllowedStatuses(order.status).length === 0">
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

                  <button
                    *ngIf="order.status === 'SHIPPED' || order.status === 'DELIVERED'"
                    mat-icon-button
                    class="delivery-btn"
                    (click)="openDeliveryModal(order)"
                    matTooltip="Manage delivery tracking">
                    <mat-icon>local_shipping</mat-icon>
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

      <!-- Delivery Management Modal -->
      <div *ngIf="deliveryOrder" class="update-modal-backdrop">
        <div class="update-card delivery-modal surface-card">
          <div class="modal-header">
            <div>
              <h3>Delivery for Order #{{ deliveryOrder.id }}</h3>
              <small>{{ deliveryOrder.shippingAddress }}</small>
            </div>
            <button mat-icon-button type="button" (click)="closeDeliveryModal()">
              <mat-icon>close</mat-icon>
            </button>
          </div>

          <div *ngIf="deliveryLoading" class="delivery-modal-loading">
            <mat-spinner diameter="36"></mat-spinner>
          </div>

          <ng-container *ngIf="!deliveryLoading">
            <app-delivery-timeline *ngIf="delivery" [delivery]="delivery" />

            <form
              *ngIf="(!delivery && deliveryOrder.status === 'SHIPPED') || delivery?.status === 'PENDING'"
              [formGroup]="deliveryForm"
              (ngSubmit)="saveDeliveryDetails()"
              class="delivery-form">
              <h4>{{ delivery ? 'Edit pending delivery' : 'Create delivery tracking' }}</h4>

              <div class="form-grid">
                <mat-form-field appearance="outline">
                  <mat-label>Carrier</mat-label>
                  <input matInput formControlName="carrier" maxlength="100" placeholder="e.g. GHN">
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Tracking code</mat-label>
                  <input matInput formControlName="trackingCode" maxlength="100">
                </mat-form-field>
              </div>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Estimated delivery</mat-label>
                <input matInput type="date" formControlName="estimatedDelivery" [min]="minEstimatedDelivery">
              </mat-form-field>

              <div class="modal-actions">
                <button mat-raised-button class="btn-solid-primary" type="submit"
                        [disabled]="deliveryForm.invalid || deliverySaving">
                  <mat-spinner *ngIf="deliverySaving" diameter="20"></mat-spinner>
                  <span *ngIf="!deliverySaving">{{ delivery ? 'Save details' : 'Create delivery' }}</span>
                </button>
              </div>
            </form>

            <section *ngIf="delivery && delivery.status !== 'DELIVERED'" class="delivery-status-panel">
              <h4>Record delivery progress</h4>

              <div class="milestone-actions">
                <button *ngIf="delivery.status === 'PENDING'" mat-stroked-button type="button"
                        (click)="recordMilestone('READY_FOR_PICKUP')" [disabled]="deliverySaving">
                  Ready for pickup
                </button>
                <button *ngIf="delivery.status === 'IN_TRANSIT'" mat-stroked-button type="button"
                        (click)="recordMilestone('OUT_FOR_DELIVERY')" [disabled]="deliverySaving">
                  Out for delivery
                </button>
              </div>

              <form [formGroup]="deliveryStatusForm" (ngSubmit)="saveDeliveryStatus()">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Next delivery status</mat-label>
                  <mat-select formControlName="status">
                    <mat-option *ngFor="let status of getNextDeliveryStatuses(delivery.status)" [value]="status">
                      {{ status.replace('_', ' ') }}
                    </mat-option>
                  </mat-select>
                </mat-form-field>

                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Milestone note (optional)</mat-label>
                  <textarea matInput rows="2" maxlength="1000" formControlName="note"></textarea>
                </mat-form-field>

                <div class="modal-actions">
                  <button mat-raised-button class="btn-solid-primary" type="submit"
                          [disabled]="deliveryStatusForm.invalid || deliverySaving">
                    Update status
                  </button>
                </div>
              </form>
            </section>

            <div *ngIf="!delivery && deliveryOrder.status === 'DELIVERED'" class="legacy-delivery-empty">
              This order was delivered before tracking details were recorded.
            </div>
          </ng-container>
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
    .delivery-btn { color: #0284c7; }

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
    .delivery-modal { width: 760px; max-height: 90vh; overflow-y: auto; }
    .delivery-modal-loading { display: flex; justify-content: center; padding: 48px; }
    .delivery-form, .delivery-status-panel { margin-top: 20px; padding-top: 18px; border-top: 1px solid var(--border-subtle); }
    .delivery-form h4, .delivery-status-panel h4 { margin: 0 0 14px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .full-width { width: 100%; }
    .milestone-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }
    .legacy-delivery-empty { padding: 28px; text-align: center; color: var(--text-muted); }
    @media (max-width: 680px) { .form-grid { grid-template-columns: 1fr; } }
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
  deliveryOrder: Order | null = null;
  delivery: Delivery | null = null;
  deliveryLoading = false;
  deliverySaving = false;
  readonly minEstimatedDelivery = this.localDateString(new Date());
  deliveryForm: FormGroup;
  deliveryStatusForm: FormGroup;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private adminOrderService: AdminOrderService,
    private deliveryService: DeliveryService,
    private notificationService: NotificationService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.updateForm = this.fb.group({
      status: ['', Validators.required],
      note: ['']
    });
    this.deliveryForm = this.fb.group({
      carrier: ['', [Validators.required, Validators.maxLength(100)]],
      trackingCode: ['', [Validators.required, Validators.maxLength(100)]],
      estimatedDelivery: ['', Validators.required]
    });
    this.deliveryStatusForm = this.fb.group({
      status: ['', Validators.required],
      note: ['', Validators.maxLength(1000)]
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
      case 'SHIPPED': return [];
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

  openDeliveryModal(order: Order): void {
    this.deliveryOrder = order;
    this.delivery = null;
    this.deliveryForm.reset();
    this.deliveryStatusForm.reset();
    this.deliveryLoading = true;
    this.deliveryService.getAdminDelivery(order.id).subscribe({
      next: (response) => {
        this.deliveryLoading = false;
        if (response.data) {
          this.applyDelivery(response.data);
        }
      },
      error: (error) => {
        this.deliveryLoading = false;
        if (error.status !== 404) {
          this.notificationService.error(error.error?.message || 'Failed to load delivery tracking');
        }
      }
    });
  }

  closeDeliveryModal(): void {
    this.deliveryOrder = null;
    this.delivery = null;
    this.deliveryForm.reset();
    this.deliveryStatusForm.reset();
  }

  saveDeliveryDetails(): void {
    if (this.deliveryForm.invalid || !this.deliveryOrder) {
      return;
    }
    this.deliverySaving = true;
    const request = {
      carrier: String(this.deliveryForm.value.carrier).trim(),
      trackingCode: String(this.deliveryForm.value.trackingCode).trim(),
      estimatedDelivery: String(this.deliveryForm.value.estimatedDelivery)
    };
    const isUpdate = Boolean(this.delivery);
    const operation = this.delivery
      ? this.deliveryService.update(this.delivery.id, request)
      : this.deliveryService.create(this.deliveryOrder.id, request);

    operation.subscribe({
      next: (response) => {
        this.deliverySaving = false;
        if (response.data) {
          this.applyDelivery(response.data);
          this.notificationService.success(isUpdate ? 'Delivery details saved' : 'Delivery created');
        }
      },
      error: (error) => {
        this.deliverySaving = false;
        this.notificationService.error(error.error?.message || 'Failed to save delivery');
      }
    });
  }

  saveDeliveryStatus(): void {
    if (!this.delivery || this.deliveryStatusForm.invalid) {
      return;
    }
    this.deliverySaving = true;
    const status = this.deliveryStatusForm.value.status as DeliveryStatus;
    const eventType = status === 'IN_TRANSIT' && this.delivery.status === 'PENDING'
      ? 'PICKED_UP' as DeliveryEventType
      : undefined;
    this.deliveryService.updateStatus(this.delivery.id, {
      expectedVersion: this.delivery.version,
      status,
      eventType,
      note: this.deliveryStatusForm.value.note || undefined
    }).subscribe({
      next: (response) => {
        this.deliverySaving = false;
        if (response.data) {
          this.applyDelivery(response.data);
          this.notificationService.success(`Delivery updated to ${status}`);
          if (status === 'DELIVERED') {
            this.loadOrders();
          }
        }
      },
      error: (error) => {
        this.deliverySaving = false;
        this.notificationService.error(error.error?.message || 'Failed to update delivery status');
      }
    });
  }

  recordMilestone(eventType: DeliveryEventType): void {
    if (!this.delivery) {
      return;
    }
    this.deliverySaving = true;
    this.deliveryService.addEvent(this.delivery.id, {
      requestId: crypto.randomUUID(),
      eventType
    }).subscribe({
      next: (response) => {
        this.deliverySaving = false;
        if (response.data) {
          this.applyDelivery(response.data);
          this.notificationService.success('Delivery milestone recorded');
        }
      },
      error: (error) => {
        this.deliverySaving = false;
        this.notificationService.error(error.error?.message || 'Failed to record milestone');
      }
    });
  }

  getNextDeliveryStatuses(status: DeliveryStatus): DeliveryStatus[] {
    switch (status) {
      case 'PENDING': return ['IN_TRANSIT', 'FAILED'];
      case 'IN_TRANSIT': return ['DELIVERED', 'FAILED'];
      case 'FAILED': return ['IN_TRANSIT'];
      case 'DELIVERED': return [];
    }
  }

  private applyDelivery(delivery: Delivery): void {
    this.delivery = delivery;
    this.deliveryForm.patchValue({
      carrier: delivery.carrier,
      trackingCode: delivery.trackingCode,
      estimatedDelivery: delivery.estimatedDelivery
    });
    const nextStatuses = this.getNextDeliveryStatuses(delivery.status);
    this.deliveryStatusForm.reset({
      status: nextStatuses[0] || '',
      note: ''
    });
  }

  private localDateString(date: Date): string {
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return local.toISOString().slice(0, 10);
  }
}
