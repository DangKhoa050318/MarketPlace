import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
import { finalize } from 'rxjs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OrderService, Order } from '../../../core/services/order.service';
import { ReviewService } from '../../../core/services/review.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { OrderReviewDialogComponent } from '../order-review-dialog/order-review-dialog.component';
import { VietQrDialogComponent } from '../../../shared/components/vietqr-dialog/vietqr-dialog.component';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
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
    MatTooltipModule,
    ConfirmDialogComponent,
    OrderReviewDialogComponent,
    VietQrDialogComponent
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
                  <a mat-icon-button class="icon-btn-action btn-view" [routerLink]="['/orders', order.id]" matTooltip="View Details">
                    <mat-icon>visibility</mat-icon>
                  </a>
                  <button 
                    *ngIf="order.status === 'PENDING' && order.paymentStatus === 'PENDING_PAYGATE' && !isSessionExpired(order) && order.paymentMethod === 'BANK_TRANSFER'" 
                    mat-icon-button 
                    class="icon-btn-action btn-vietqr" 
                    (click)="openVietQrModal(order)"
                    matTooltip="Pay via VietQR">
                    <mat-icon>qr_code_2</mat-icon>
                  </button>
                  <button 
                    *ngIf="order.status === 'PENDING' && order.paymentStatus === 'PENDING_PAYGATE' && !isSessionExpired(order) && order.paymentMethod !== 'BANK_TRANSFER'" 
                    mat-icon-button 
                    class="icon-btn-action btn-paygate" 
                    (click)="continuePaygatePayment(order)"
                    matTooltip="Pay via PayGate">
                    <mat-icon>payment</mat-icon>
                  </button>
                  <button
                    *ngIf="order.status === 'SHIPPED'"
                    mat-icon-button
                    class="icon-btn-action btn-delivered"
                    (click)="onConfirmReceived(order)"
                    matTooltip="Confirm Order Received">
                    <mat-icon>check_circle</mat-icon>
                  </button>
                  <button
                    *ngIf="canRequestReturn(order)"
                    mat-stroked-button
                    color="accent"
                    class="btn-return-refund"
                    (click)="openReturnRequestForm(order)"
                    title="Request return/refund after delivery review">
                    <mat-icon>assignment_return</mat-icon>
                    <span>Return / refund</span>
                  </button>
                  <span
                    *ngIf="order.returnRequestStatus"
                    class="return-status-chip"
                    [title]="'Return/refund request status: ' + returnStatusLabel(order.returnRequestStatus)">
                    <mat-icon>assignment_turned_in</mat-icon>
                    {{ returnStatusLabel(order.returnRequestStatus) }}
                  </span>
                  <span
                    *ngIf="!order.returnRequestStatus && order.refundRequestStatus"
                    class="refund-status-chip"
                    [title]="'Refund request status: ' + refundStatusLabel(order.refundRequestStatus)">
                    <mat-icon>payments</mat-icon>
                    {{ refundStatusLabel(order.refundRequestStatus) }}
                  </span>
                  <button
                    *ngIf="hasUnreviewedItems(order)"
                    mat-icon-button
                    class="icon-btn-action btn-review"
                    (click)="openOrderReviewModal(order)"
                    matTooltip="Review Purchased Items">
                    <mat-icon>rate_review</mat-icon>
                  </button>
                  <button 
                    *ngIf="canCancelOrder(order)" 
                    mat-icon-button 
                    class="icon-btn-action btn-cancel"
                    (click)="onCancelOrder(order)" 
                    matTooltip="Cancel Order">
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

      <div *ngIf="selectedReturnOrder" class="return-modal-backdrop">
        <form class="return-modal" (ngSubmit)="submitReturnRequest(selectedReturnOrder)">
          <div class="return-modal-header">
            <div>
              <h2>Return and Refund Request</h2>
              <p>Order #{{ selectedReturnOrder.id }} · use this only after shipping/delivery when you need to return goods or ask admin to review a refund.</p>
            </div>
            <button mat-icon-button type="button" (click)="closeReturnRequestForm()" aria-label="Close return request form">
              <mat-icon>close</mat-icon>
            </button>
          </div>

          <div class="return-modal-body">
            <label for="return-item">Item to return</label>
            <select
              id="return-item"
              name="returnItem"
              [(ngModel)]="returnOrderItemId"
              (ngModelChange)="useMaxReturnQuantity(selectedReturnOrder)">
              <option [ngValue]="null">Entire order</option>
              <option *ngFor="let item of refundableItems(selectedReturnOrder)" [ngValue]="item.id">
                {{ item.productName }} - refundable quantity {{ refundableQuantity(item) }}
              </option>
            </select>

            <div class="return-quantity-summary">
              <strong>Return quantity</strong>
              <span>{{ returnQuantityLabel(selectedReturnOrder) }}</span>
            </div>

            <label for="return-reason">Return request reason</label>
            <textarea
              id="return-reason"
              name="returnReason"
              [(ngModel)]="returnReason"
              rows="5"
              maxlength="1000"
              placeholder="Describe the issue clearly: defective item, wrong product, missing parts, damaged packaging, or delivery condition."></textarea>

            <div class="return-evidence-upload">
              <div>
                <strong>Evidence images</strong>
                <p>Upload up to 5 clear photos for admin review.</p>
              </div>
              <input #returnEvidenceInput type="file" accept="image/*" multiple hidden (change)="uploadReturnEvidence($event)">
              <button mat-stroked-button type="button" (click)="returnEvidenceInput.click()" [disabled]="uploadingReturnEvidence || returnEvidenceImageUrls.length >= 5">
                <mat-icon>add_photo_alternate</mat-icon>
                {{ uploadingReturnEvidence ? 'Uploading images...' : 'Upload evidence images' }}
              </button>
              <div class="return-evidence-grid" *ngIf="returnEvidenceImageUrls.length">
                <div class="return-evidence-thumb" *ngFor="let imageUrl of returnEvidenceImageUrls">
                  <img [src]="imageUrl" alt="Return evidence image">
                  <button mat-icon-button type="button" (click)="removeReturnEvidence(imageUrl)" aria-label="Remove evidence image">
                    <mat-icon>close</mat-icon>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="return-modal-actions">
            <button mat-button type="button" (click)="closeReturnRequestForm()" [disabled]="requestingReturn">Cancel</button>
            <button mat-raised-button color="primary" type="submit" [disabled]="!returnReason.trim() || requestingReturn">
              {{ requestingReturn ? 'Submitting return/refund request...' : 'Submit return/refund request' }}
            </button>
          </div>
        </form>
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
      align-items: center;
      flex-wrap: wrap;
      gap: 6px;
    }
    .btn-return-refund {
      min-height: 36px;
      border-color: rgba(22, 163, 74, 0.35);
      color: #15803d;
      white-space: nowrap;
    }
    .btn-return-refund mat-icon {
      margin-right: 4px;
    }
    .return-modal-backdrop {
      position: fixed;
      inset: 0;
      z-index: 1200;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: rgba(15, 23, 42, 0.58);
    }
    .return-modal {
      width: min(680px, calc(100vw - 32px));
      max-height: min(820px, calc(100vh - 32px));
      display: flex;
      flex-direction: column;
      overflow: hidden;
      border-radius: 10px;
      background: #ffffff;
      box-shadow: 0 24px 70px rgba(15, 23, 42, 0.28);
      border: 1px solid #e2e8f0;
    }
    .return-modal-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      padding: 20px 22px;
      border-bottom: 1px solid #e2e8f0;
    }
    .return-modal-header h2 {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 850;
      color: #0f172a;
    }
    .return-modal-header p {
      margin: 5px 0 0;
      color: #64748b;
      font-size: 0.92rem;
    }
    .return-modal-body {
      display: flex;
      flex-direction: column;
      gap: 10px;
      padding: 18px 22px;
      overflow-y: auto;
    }
    .return-modal-body label {
      font-size: 0.82rem;
      font-weight: 800;
      color: #334155;
    }
    .return-modal-body select,
    .return-modal-body textarea {
      width: 100%;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      padding: 10px 12px;
      font: inherit;
      box-sizing: border-box;
      background: #ffffff;
    }
    .return-modal-body textarea {
      min-height: 132px;
      resize: vertical;
    }
    .return-quantity-summary {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 11px 12px;
      border: 1px solid #dbe4ee;
      border-radius: 8px;
      background: #f8fafc;
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
    .return-evidence-upload {
      display: flex;
      flex-direction: column;
      gap: 10px;
      padding: 12px;
      border: 1px dashed #94a3b8;
      border-radius: 8px;
      background: #f8fafc;
    }
    .return-evidence-upload strong {
      font-size: 0.88rem;
      color: #334155;
    }
    .return-evidence-upload p {
      margin: 4px 0 0;
      color: #64748b;
      font-size: 0.84rem;
    }
    .return-evidence-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(92px, 1fr));
      gap: 10px;
    }
    .return-evidence-thumb {
      position: relative;
      aspect-ratio: 1;
      border-radius: 8px;
      overflow: hidden;
      border: 1px solid #cbd5e1;
      background: #e2e8f0;
    }
    .return-evidence-thumb img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .return-evidence-thumb button {
      position: absolute;
      top: 4px;
      right: 4px;
      width: 26px;
      height: 26px;
      background: rgba(15, 23, 42, 0.72);
      color: #ffffff;
    }
    .return-modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      padding: 16px 22px;
      border-top: 1px solid #e2e8f0;
      background: #f8fafc;
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
    .return-status-chip {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      min-height: 34px;
      padding: 0 10px;
      border-radius: 6px;
      background: #ecfdf5;
      color: #166534;
      border: 1px solid rgba(22, 163, 74, 0.25);
      font-size: 0.78rem;
      font-weight: 800;
      white-space: nowrap;
    }
    .return-status-chip mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }
    .refund-status-chip {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      min-height: 34px;
      padding: 0 10px;
      border-radius: 6px;
      background: #eff6ff;
      color: #1d4ed8;
      border: 1px solid rgba(37, 99, 235, 0.25);
      font-size: 0.78rem;
      font-weight: 800;
      white-space: nowrap;
    }
    .refund-status-chip mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
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

    .action-buttons {
      display: flex;
      align-items: center;
      justify-content: flex-center;
      gap: 6px;
    }
    .icon-btn-action {
      width: 34px !important;
      height: 34px !important;
      line-height: 34px !important;
      border-radius: 50% !important;
      display: inline-flex !important;
      align-items: center !important;
      justify-content: center !important;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;
      padding: 0 !important;
    }
    .icon-btn-action mat-icon {
      font-size: 18px !important;
      width: 18px !important;
      height: 18px !important;
      margin: 0 !important;
    }
    .btn-view {
      color: var(--accent-cyan, #00f2fe) !important;
      background: rgba(0, 242, 254, 0.1) !important;
    }
    .btn-view:hover {
      background: rgba(0, 242, 254, 0.3) !important;
      transform: translateY(-2px);
    }
    .btn-vietqr {
      color: #ff2d87 !important;
      background: rgba(255, 45, 135, 0.12) !important;
    }
    .btn-vietqr:hover {
      background: rgba(255, 45, 135, 0.3) !important;
      transform: translateY(-2px);
    }
    .btn-paygate {
      color: #38bdf8 !important;
      background: rgba(56, 189, 248, 0.12) !important;
    }
    .btn-paygate:hover {
      background: rgba(56, 189, 248, 0.3) !important;
      transform: translateY(-2px);
    }
    .btn-delivered {
      color: #22c55e !important;
      background: rgba(34, 197, 94, 0.12) !important;
    }
    .btn-delivered:hover {
      background: rgba(34, 197, 94, 0.3) !important;
      transform: translateY(-2px);
    }
    .btn-review {
      color: #eab308 !important;
      background: rgba(234, 179, 8, 0.12) !important;
    }
    .btn-review:hover {
      background: rgba(234, 179, 8, 0.3) !important;
      transform: translateY(-2px);
    }
    .btn-cancel {
      color: #ef4444 !important;
      background: rgba(239, 68, 68, 0.12) !important;
    }
    .btn-cancel:hover {
      background: rgba(239, 68, 68, 0.3) !important;
      transform: translateY(-2px);
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
  selectedReturnOrder: Order | null = null;
  returnOrderItemId: number | null = null;
  returnQuantity = 1;
  returnReason = '';
  returnEvidenceImageUrls: string[] = [];
  uploadingReturnEvidence = false;
  requestingReturn = false;

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

  getRemainingSessionSeconds(order: Order): number {
    if (!order.paygateExpiresAt) return 900;
    const expires = new Date(order.paygateExpiresAt).getTime();
    const now = new Date().getTime();
    const diff = Math.floor((expires - now) / 1000);
    return diff > 0 ? diff : 0;
  }

  isSessionExpired(order: Order): boolean {
    if (order.status === 'CANCELLED') return true;
    if (order.status === 'PENDING' && order.paygateExpiresAt) {
      return this.getRemainingSessionSeconds(order) <= 0;
    }
    return false;
  }

  continuePaygatePayment(order: Order): void {
    const url = order.paygatePayload?.paymentUrl;
    if (url) {
      window.location.href = url;
    } else {
      this.notification.error('PayGate payment URL not available');
    }
  }

  openVietQrModal(order: Order): void {
    const usdAmount = order.totalAmount || 0;
    const vndAmount = Math.round(usdAmount * 25400);
    const pg = order.paygatePayload;
    const timerSecs = this.getRemainingSessionSeconds(order);
    const dialogRef = this.dialog.open(VietQrDialogComponent, {
      width: '840px',
      maxWidth: '95vw',
      panelClass: 'paygate-vqr-dialog-panel',
      data: {
        orderId: order.id,
        amountVnd: vndAmount,
        description: pg?.transferContent || `ORD-${order.id}`,
        qrPayload: pg?.qrPayload,
        bankName: pg?.bankAccount?.bankName,
        accountNo: pg?.bankAccount?.accountNumber,
        accountName: pg?.bankAccount?.accountHolder,
        timerSeconds: timerSecs > 0 ? timerSecs : 900
      }
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        this.orderService.confirmVietQrPayment(order.id).subscribe({
          next: (res) => {
            if (res.success) {
              this.notification.success(`Payment confirmed! Order #${order.id} is now CONFIRMED.`);
              this.loadOrders();
            }
          },
          error: (err) => {
            this.notification.error(err?.error?.message || 'Failed to confirm payment');
          }
        });
      } else if (result === 'CANCEL') {
        this.orderService.cancelVietQrPayment(order.id).subscribe({
          next: (res) => {
            if (res.success) {
              this.notification.info(`Payment cancelled. Order #${order.id} has been CANCELLED and stock released.`);
              this.loadOrders();
            }
          },
          error: (err) => {
            this.notification.error(err?.error?.message || 'Failed to cancel payment');
          }
        });
      }
    });
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
    const paidOnline = order.paymentStatus === 'PAID' && order.paymentMethod !== 'COD';
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: paidOnline ? 'Cancel & Refund Order' : 'Cancel Order',
        message: paidOnline
          ? `Order #${order.id} has been paid. Cancelling now will request a PayGate refund and release reserved stock.`
          : `Are you sure you want to cancel Order #${order.id}?`
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.orderService.cancelOrder(order.id).subscribe({
          next: () => {
            this.notification.success(paidOnline
              ? `Order #${order.id} cancelled and refund requested`
              : `Order #${order.id} cancelled successfully`);
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

  onConfirmReceived(order: Order): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Confirm received order',
        message: `Confirm that you received Order #${order.id}?`
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.orderService.confirmReceived(order.id).subscribe({
          next: () => {
            this.notification.success(`Order #${order.id} marked as received`);
            this.loadOrders();
          },
          error: (err) => {
            this.notification.error(err.error?.message || 'Unable to update order status');
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
          this.notification.info('Redirecting to PayGate payment...');
          window.location.href = targetUrl;
        } else {
          this.notification.error('Unable to create a new payment session. Please try again later.');
        }
      },
      error: (err) => {
        this.retryingOrderId = null;
        this.notification.error(err.error?.message || 'Unable to connect to the payment system.');
      }
    });
  }

  openReturnRequestForm(order: Order): void {
    this.selectedReturnOrder = order;
    this.returnOrderItemId = null;
    this.returnQuantity = 1;
    this.returnReason = '';
    this.returnEvidenceImageUrls = [];
  }

  closeReturnRequestForm(): void {
    this.selectedReturnOrder = null;
    this.returnOrderItemId = null;
    this.returnQuantity = 1;
    this.returnReason = '';
    this.returnEvidenceImageUrls = [];
  }

  submitReturnRequest(order: Order): void {
    const reason = this.returnReason.trim();
    if (!reason) {
      this.notification.error('Return request reason is required');
      return;
    }
    const item = this.returnOrderItemId
      ? this.refundableItems(order).find(candidate => candidate.id === this.returnOrderItemId)
      : undefined;
    const quantity = item ? this.refundableQuantity(item) : undefined;
    if (item && (!quantity || quantity <= 0 || quantity > this.refundableQuantity(item))) {
      this.notification.error(`Return quantity must be between 1 and ${this.refundableQuantity(item)}`);
      return;
    }
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Create Return Request',
        message: `Submit a return request for Order #${order.id} with the reason you entered?`
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.requestingReturn = true;
        this.orderService.createReturnRequest(order.id, {
          orderItemId: item?.id,
          quantity,
          reason,
          evidenceImageUrls: this.returnEvidenceImageUrls
        }).subscribe({
          next: () => {
            this.requestingReturn = false;
            this.notification.success(`Return request created for Order #${order.id}`);
            this.closeReturnRequestForm();
            this.loadOrders();
          },
          error: (err) => {
            this.requestingReturn = false;
            this.notification.error(err.error?.message || 'Failed to create return request');
          }
        });
      }
    });
  }

  canCancelOrder(order: Order): boolean {
    return order.status === 'PENDING' || order.status === 'CONFIRMED' || order.status === 'PROCESSING';
  }

  canRequestReturn(order: Order): boolean {
    return !order.returnRequestStatus && (order.status === 'SHIPPED' || order.status === 'DELIVERED');
  }

  returnStatusLabel(status: string): string {
    switch (status) {
      case 'REQUESTED': return 'Return requested';
      case 'APPROVED': return 'Return approved';
      case 'RETURN_RECEIVED': return 'Return received';
      case 'QC_PASSED': return 'QC passed, refund pending';
      case 'QC_FAILED': return 'QC failed';
      case 'COMPLETED': return 'Refund completed';
      case 'REJECTED': return 'Return rejected';
      default: return status;
    }
  }

  refundStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING': return 'Refund pending';
      case 'SUCCEEDED': return 'Refund completed';
      case 'FAILED': return 'Refund failed';
      default: return status;
    }
  }

  refundableItems(order: Order): Order['items'] {
    return (order.items || []).filter(item => this.refundableQuantity(item) > 0);
  }

  refundableQuantity(item: Order['items'][number]): number {
    return item.quantity - (item.refundedQuantity || 0);
  }

  useMaxReturnQuantity(order: Order): void {
    const item = this.returnOrderItemId
      ? this.refundableItems(order).find(candidate => candidate.id === this.returnOrderItemId)
      : undefined;
    this.returnQuantity = item ? this.refundableQuantity(item) : 1;
  }

  returnQuantityLabel(order: Order): string {
    const item = this.returnOrderItemId
      ? this.refundableItems(order).find(candidate => candidate.id === this.returnOrderItemId)
      : undefined;
    if (item) {
      return `All ${this.refundableQuantity(item)} item(s)`;
    }
    const total = this.refundableItems(order).reduce((sum, current) => sum + this.refundableQuantity(current), 0);
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

  cancelTitle(order: Order): string {
    if (order.paymentStatus === 'PENDING_PAYGATE') {
      return 'Cancel unpaid PayGate session';
    }
    if (order.paymentStatus === 'PAID' && order.paymentMethod !== 'COD') {
      return 'Cancel and refund via PayGate';
    }
    if (order.paymentStatus === 'REFUND_PENDING') {
      return 'Refund pending in PayGate';
    }
    return 'Cancel Order';
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
