import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { Order, OrderItem } from '../../../core/models/order.model';
import { ReviewService } from '../../../core/services/review.service';
import { NotificationService } from '../../../core/services/notification.service';

export interface OrderReviewDialogData {
  order: Order;
  reviewedOrderItemIds: Set<number>;
}

interface ItemReviewState {
  item: OrderItem;
  reviewed: boolean;
  rating: number;
  hoverRating: number;
  title: string;
  content: string;
  imageUrl: string;
  uploadingImage: boolean;
  submitting: boolean;
  submittedSuccess: boolean;
  error?: string;
}

@Component({
  selector: 'app-order-review-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="review-dialog-container">
      <div class="dialog-header">
        <h2 mat-dialog-title class="title-row">
          <mat-icon class="header-icon">rate_review</mat-icon>
          <span>Review Products - Order #{{ data.order.id }}</span>
        </h2>
        <button mat-icon-button mat-dialog-close class="close-btn">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content class="dialog-body">
        <p class="subtitle">Share your experience with the purchased products to help other shoppers!</p>

        <div class="items-list">
          <div *ngFor="let state of itemStates; let i = index" class="item-card glass-card">
            <!-- Product Header Info -->
            <div class="item-header">
              <div class="item-info">
                <span class="product-name">{{ state.item.productName }}</span>
                <span *ngIf="state.item.variantName" class="variant-chip">{{ state.item.variantName }}</span>
                <span *ngIf="state.item.sku" class="sku-code">SKU: {{ state.item.sku }}</span>
              </div>

              <!-- Reviewed Badge or Form Toggle -->
              <div class="item-status">
                <span *ngIf="state.reviewed || state.submittedSuccess" class="badge-reviewed">
                  <mat-icon>check_circle</mat-icon> Reviewed
                </span>
                <span *ngIf="!state.reviewed && !state.submittedSuccess" class="badge-pending">
                  Pending Review
                </span>
              </div>
            </div>

            <!-- Review Form for unreviewed item -->
            <div *ngIf="!state.reviewed && !state.submittedSuccess" class="review-form-box">
              <!-- Rating Stars -->
              <div class="star-rating-row">
                <span class="rating-label">Satisfaction Level:</span>
                <div class="stars">
                  <mat-icon
                    *ngFor="let star of [1, 2, 3, 4, 5]"
                    class="star-icon"
                    [class.filled]="star <= (state.hoverRating || state.rating)"
                    (mouseenter)="state.hoverRating = star"
                    (mouseleave)="state.hoverRating = 0"
                    (click)="state.rating = star">
                    star
                  </mat-icon>
                </div>
                <span class="rating-text" *ngIf="state.rating > 0">
                  {{ getRatingText(state.rating) }}
                </span>
              </div>

              <!-- Form Inputs -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Review Title (optional)</mat-label>
                <input matInput [(ngModel)]="state.title" maxlength="100"
                       placeholder="e.g., Excellent quality, super fast delivery...">
                <mat-hint align="end">{{ state.title.length }}/100</mat-hint>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Detailed Review (optional)</mat-label>
                <textarea matInput [(ngModel)]="state.content" maxlength="1000"
                          rows="3" placeholder="Share your overall experience using this product..."></textarea>
                <mat-hint align="end">{{ state.content.length }}/1000</mat-hint>
              </mat-form-field>

              <!-- Image File Upload Section -->
              <div class="image-upload-section">
                <input
                  type="file"
                  #fileInput
                  accept="image/*"
                  (change)="onFileSelected($event, state)"
                  style="display: none">

                <div *ngIf="!state.imageUrl && !state.uploadingImage" class="upload-btn-box">
                  <button mat-stroked-button type="button" class="btn-attach" (click)="fileInput.click()">
                    <mat-icon>add_a_photo</mat-icon> Attach Image File
                  </button>
                  <span class="upload-tip">Upload photo from computer (JPG, PNG, WEBP)</span>
                </div>

                <div *ngIf="state.uploadingImage" class="upload-spinner-box">
                  <mat-spinner diameter="20"></mat-spinner>
                  <span>Uploading image file...</span>
                </div>

                <div *ngIf="state.imageUrl && !state.uploadingImage" class="img-preview-container">
                  <img [src]="state.imageUrl" alt="Uploaded review image" class="uploaded-img">
                  <button mat-icon-button color="warn" type="button" class="btn-remove-img" (click)="removeImage(state)" title="Remove image">
                    <mat-icon>delete</mat-icon>
                  </button>
                </div>
              </div>

              <div *ngIf="state.error" class="error-msg">
                <mat-icon>error_outline</mat-icon> {{ state.error }}
              </div>

              <div class="form-actions">
                <button mat-raised-button color="primary" class="btn-submit"
                        [disabled]="state.submitting || state.uploadingImage || state.rating === 0"
                        (click)="submitItemReview(state)">
                  <mat-spinner *ngIf="state.submitting" diameter="18"></mat-spinner>
                  <mat-icon *ngIf="!state.submitting">send</mat-icon>
                  <span>{{ state.submitting ? 'Submitting...' : 'Submit Review' }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="dialog-footer">
        <button mat-button (click)="onClose()">Close</button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .review-dialog-container {
      padding: 8px 12px;
      max-width: 640px;
    }
    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 12px;
      border-bottom: 1px solid rgba(226, 232, 240, 0.6);
    }
    .title-row {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 1.25rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0;
    }
    .header-icon {
      color: #0284c7;
    }
    .subtitle {
      color: #64748b;
      font-size: 0.88rem;
      margin: 12px 0 18px;
    }
    .dialog-body {
      max-height: 65vh !important;
      overflow-y: auto !important;
    }
    .items-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .item-card {
      padding: 16px;
      border-radius: 14px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
    }
    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;
    }
    .item-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .product-name {
      font-weight: 800;
      font-size: 1rem;
      color: #0f172a;
    }
    .variant-chip {
      display: inline-block;
      align-self: flex-start;
      font-size: 0.78rem;
      font-weight: 700;
      background: #eef2ff;
      color: #4f46e5;
      padding: 2px 8px;
      border-radius: 6px;
    }
    .sku-code {
      font-size: 0.76rem;
      color: #64748b;
    }
    .badge-reviewed {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: #059669;
      background: #ecfdf5;
      padding: 4px 10px;
      border-radius: 12px;
      font-weight: 700;
      font-size: 0.8rem;
      border: 1px solid rgba(16, 185, 129, 0.3);
    }
    .badge-reviewed mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }
    .badge-pending {
      color: #d97706;
      background: #fffbeb;
      padding: 4px 10px;
      border-radius: 12px;
      font-weight: 700;
      font-size: 0.8rem;
      border: 1px solid rgba(217, 119, 6, 0.3);
    }
    .review-form-box {
      margin-top: 14px;
      padding-top: 14px;
      border-top: 1px dashed #cbd5e1;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .star-rating-row {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 6px;
    }
    .rating-label {
      font-weight: 700;
      font-size: 0.86rem;
      color: #334155;
    }
    .stars {
      display: flex;
      gap: 4px;
    }
    .star-icon {
      font-size: 24px;
      width: 24px;
      height: 24px;
      color: #cbd5e1;
      cursor: pointer;
      transition: transform 0.15s ease, color 0.15s ease;
    }
    .star-icon:hover, .star-icon.filled {
      color: #f59e0b;
      transform: scale(1.15);
    }
    .rating-text {
      font-size: 0.82rem;
      font-weight: 700;
      color: #d97706;
    }
    .full-width {
      width: 100%;
    }
    .image-upload-section {
      margin-bottom: 12px;
    }
    .upload-btn-box {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .btn-attach {
      border-color: #cbd5e1 !important;
      color: #475569 !important;
      font-weight: 600;
      border-radius: 8px;
    }
    .upload-tip {
      font-size: 0.78rem;
      color: #94a3b8;
    }
    .upload-spinner-box {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 0.84rem;
      color: #0284c7;
    }
    .img-preview-container {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 6px;
      background: #fff;
      border: 1px solid #cbd5e1;
      border-radius: 10px;
    }
    .uploaded-img {
      width: 72px;
      height: 72px;
      object-fit: cover;
      border-radius: 6px;
    }
    .btn-remove-img {
      width: 32px;
      height: 32px;
      line-height: 32px;
    }
    .error-msg {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #dc2626;
      font-size: 0.82rem;
    }
    .form-actions {
      display: flex;
      justify-content: flex-end;
    }
    .btn-submit {
      background: #0284c7 !important;
      color: #fff !important;
      font-weight: 700;
      display: inline-flex !important;
      align-items: center !important;
      justify-content: center !important;
      height: 38px !important;
      padding: 0 18px !important;
      border-radius: 8px !important;
    }
    :host ::ng-deep .btn-submit .mdc-button__label {
      display: inline-flex !important;
      align-items: center !important;
      justify-content: center !important;
      gap: 8px !important;
    }
    :host ::ng-deep .btn-submit .mat-mdc-progress-spinner,
    :host ::ng-deep .btn-submit .mat-spinner {
      display: inline-flex !important;
      align-items: center !important;
      justify-content: center !important;
      margin: 0 !important;
    }
    :host ::ng-deep .btn-submit .mat-mdc-progress-spinner circle,
    :host ::ng-deep .btn-submit .mat-spinner circle {
      stroke: #ffffff !important;
    }
  `]
})
export class OrderReviewDialogComponent implements OnInit {
  itemStates: ItemReviewState[] = [];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: OrderReviewDialogData,
    private dialogRef: MatDialogRef<OrderReviewDialogComponent>,
    private reviewService: ReviewService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.itemStates = (this.data.order.items || []).map(item => ({
      item,
      reviewed: item.id ? this.data.reviewedOrderItemIds.has(item.id) : false,
      rating: 5,
      hoverRating: 0,
      title: '',
      content: '',
      imageUrl: '',
      uploadingImage: false,
      submitting: false,
      submittedSuccess: false
    }));
  }

  getRatingText(rating: number): string {
    switch (rating) {
      case 5: return 'Excellent';
      case 4: return 'Good';
      case 3: return 'Average';
      case 2: return 'Fair';
      case 1: return 'Poor';
      default: return '';
    }
  }

  onFileSelected(event: Event, state: ItemReviewState): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    state.uploadingImage = true;
    state.error = undefined;

    this.reviewService.uploadImage(file).subscribe({
      next: (res) => {
        state.uploadingImage = false;
        if (res.success && res.data?.url) {
          state.imageUrl = res.data.url;
          this.notification.success('Image uploaded successfully');
        }
      },
      error: (err) => {
        state.uploadingImage = false;
        state.error = err.error?.message || 'Failed to upload image. Please try again.';
      }
    });
  }

  removeImage(state: ItemReviewState): void {
    state.imageUrl = '';
  }

  submitItemReview(state: ItemReviewState): void {
    const productId = state.item.productId;
    if (!productId) {
      state.error = 'Product information is missing for this item.';
      return;
    }

    state.submitting = true;
    state.error = undefined;

    this.reviewService.create(productId, {
      rating: state.rating,
      title: state.title.trim() || undefined,
      content: state.content.trim() || undefined,
      imageUrl: state.imageUrl.trim() || undefined,
      orderItemId: state.item.id
    }).subscribe({
      next: () => {
        state.submitting = false;
        state.submittedSuccess = true;
        this.data.reviewedOrderItemIds.add(state.item.id);
        this.notification.success(`Review submitted for "${state.item.productName}"`);
      },
      error: (err) => {
        state.submitting = false;
        state.error = err.error?.message || 'Failed to submit review. Please try again.';
      }
    });
  }

  onClose(): void {
    this.dialogRef.close({
      reviewedOrderItemIds: this.data.reviewedOrderItemIds
    });
  }
}
