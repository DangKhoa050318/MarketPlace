import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { finalize } from 'rxjs/operators';
import { ProductReview } from '../../../../core/models/review.model';
import { NotificationService } from '../../../../core/services/notification.service';
import { ReviewService } from '../../../../core/services/review.service';

@Component({
  selector: 'app-admin-review-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatProgressSpinnerModule, MatSelectModule
  ],
  template: `
    <section class="admin-reviews">
      <header class="page-header">
        <div>
          <span class="eyebrow">Content Moderation</span>
          <h1 class="page-title"><mat-icon>reviews</mat-icon> Product Reviews</h1>
          <p class="page-subtitle">Browse customer reviews, filter by star rating, and moderate visibility.</p>
        </div>
      </header>

      <!-- Filters -->
      <div class="filter-card surface-card">
        <mat-form-field appearance="outline" class="filter-field">
          <mat-label>Rating</mat-label>
          <mat-select [(ngModel)]="rating" (selectionChange)="applyFilters()">
            <mat-option [value]="null">All ratings</mat-option>
            <mat-option *ngFor="let r of ratingOptions" [value]="r">{{ r }} star{{ r > 1 ? 's' : '' }}</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="filter-field">
          <mat-label>Status</mat-label>
          <mat-select [(ngModel)]="status" (selectionChange)="applyFilters()">
            <mat-option [value]="null">All statuses</mat-option>
            <mat-option *ngFor="let s of statusOptions" [value]="s">{{ s }}</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="filter-field">
          <mat-label>Product ID</mat-label>
          <input matInput type="number" [(ngModel)]="productId" (change)="applyFilters()" placeholder="e.g. 1">
        </mat-form-field>

        <button mat-stroked-button class="reset-btn" (click)="resetFilters()">
          <mat-icon>filter_alt_off</mat-icon> Reset
        </button>
      </div>

      <!-- States -->
      <div *ngIf="loading" class="state"><mat-spinner diameter="36"></mat-spinner></div>
      <div *ngIf="!loading && error" class="state error">
        <mat-icon>error_outline</mat-icon> {{ error }}
        <button mat-button (click)="load()">Retry</button>
      </div>
      <div *ngIf="!loading && !error && reviews.length === 0" class="state empty">
        <mat-icon>rate_review</mat-icon> No reviews match these filters.
      </div>

      <!-- Table -->
      <div *ngIf="!loading && !error && reviews.length" class="table-wrap surface-card">
        <table>
          <thead>
            <tr>
              <th>Product</th><th>Customer</th><th>Rating</th><th>Review</th>
              <th>Status</th><th>Date</th><th class="actions-col">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let rv of reviews">
              <td>
                <strong>{{ rv.productName || ('Product #' + rv.productId) }}</strong>
                <small>#{{ rv.productId }}</small>
              </td>
              <td>
                {{ rv.username }}
                <span *ngIf="rv.isVerifiedPurchase" class="verified" title="Verified purchase">
                  <mat-icon>verified</mat-icon>
                </span>
              </td>
              <td>
                <span class="stars" [attr.aria-label]="rv.rating + ' stars'">
                  <mat-icon *ngFor="let s of stars" class="star" [class.filled]="s <= rv.rating">
                    {{ s <= rv.rating ? 'star' : 'star_border' }}
                  </mat-icon>
                </span>
                <span class="rating-num">{{ rv.rating }}/5</span>
              </td>
              <td class="review-cell">
                <strong *ngIf="rv.title">{{ rv.title }}</strong>
                <p *ngIf="rv.content">{{ rv.content }}</p>
                <em *ngIf="!rv.title && !rv.content" class="rating-only">(rating only)</em>
                <div *ngIf="rv.sellerReply" class="shop-reply"><strong>Shop:</strong> {{ rv.sellerReply }}</div>
                <div *ngIf="replyingId === rv.id; else replyBtn" class="reply-box">
                  <textarea [(ngModel)]="replyText" rows="2" maxlength="1000" placeholder="Write a shop reply..."></textarea>
                  <div class="reply-actions">
                    <button mat-button (click)="cancelReply()">Cancel</button>
                    <button mat-flat-button color="primary" (click)="submitReply(rv)" [disabled]="!replyText.trim() || replySaving">Send</button>
                  </div>
                </div>
                <ng-template #replyBtn>
                  <button mat-button class="reply-toggle" (click)="startReply(rv)">
                    <mat-icon>reply</mat-icon> {{ rv.sellerReply ? 'Edit reply' : 'Reply' }}
                  </button>
                </ng-template>
              </td>
              <td>
                <span class="status-badge" [class]="rv.status.toLowerCase()">{{ rv.status }}</span>
              </td>
              <td class="date-cell">{{ rv.createdAt | date:'mediumDate' }}</td>
              <td class="actions-col">
                <button *ngIf="rv.status !== 'HIDDEN'" mat-stroked-button color="warn"
                        (click)="setStatus(rv, 'HIDDEN')" [disabled]="busyId === rv.id">
                  <mat-icon>visibility_off</mat-icon> Hide
                </button>
                <button *ngIf="rv.status === 'HIDDEN'" mat-stroked-button color="primary"
                        (click)="setStatus(rv, 'APPROVED')" [disabled]="busyId === rv.id">
                  <mat-icon>visibility</mat-icon> Approve
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <span>{{ totalElements | number }} review(s)</span>
          <button mat-icon-button (click)="changePage(-1)" [disabled]="page === 0" aria-label="Previous page">
            <mat-icon>chevron_left</mat-icon>
          </button>
          <span>Page {{ page + 1 }} of {{ totalPages || 1 }}</span>
          <button mat-icon-button (click)="changePage(1)" [disabled]="page + 1 >= totalPages" aria-label="Next page">
            <mat-icon>chevron_right</mat-icon>
          </button>
        </div>
      </div>
    </section>
  `,
  styles: [`
    :host { display: block; color: var(--text-main); }
    .admin-reviews { display: grid; gap: 20px; max-width: 1280px; margin: 0 auto; }
    .page-header { padding: 4px 0 12px; border-bottom: 1px solid #e2e8f0; }
    .eyebrow { color: #0284c7; font-size: .7rem; font-weight: 800; letter-spacing: .13em; text-transform: uppercase; }
    .page-title { display: flex; align-items: center; gap: 10px; margin: 4px 0 0; font-size: 1.8rem; font-weight: 800; color: #0f172a; }
    .page-subtitle { margin: 4px 0 0; color: #64748b; font-size: .9rem; }
    .surface-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; }
    .filter-card { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; padding: 16px 20px; }
    .filter-field { width: 200px; margin-bottom: -1.25em; }
    .reset-btn { height: 44px; }
    .state { display: flex; align-items: center; justify-content: center; gap: 10px; min-height: 160px; color: #64748b; }
    .state.error { color: #b91c1c; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; min-width: 900px; border-collapse: collapse; }
    th, td { padding: 12px 14px; border-bottom: 1px solid #eef2f7; text-align: left; font-size: .82rem; vertical-align: top; }
    th { color: #475569; background: #f8fafc; font-weight: 750; white-space: nowrap; }
    td strong { color: #0f172a; } td small { display: block; color: #94a3b8; font-size: .72rem; }
    .stars { display: inline-flex; }
    .star { width: 16px; height: 16px; font-size: 16px; color: #cbd5e1; }
    .star.filled { color: #f59e0b; }
    .rating-num { display: block; color: #64748b; font-size: .72rem; margin-top: 2px; }
    .review-cell { max-width: 320px; } .review-cell p { margin: 4px 0 0; color: #475569; font-size: .78rem; }
    .verified { color: #059669; } .verified mat-icon { font-size: 15px; width: 15px; height: 15px; vertical-align: middle; }
    .status-badge { font-size: .72rem; font-weight: 800; padding: 3px 9px; border-radius: 12px; }
    .status-badge.approved { background: #dcfce7; color: #15803d; }
    .status-badge.hidden { background: #fef3c7; color: #b45309; }
    .status-badge.deleted { background: #ffe4e6; color: #be123c; }
    .date-cell { white-space: nowrap; color: #64748b; }
    .actions-col { white-space: nowrap; }
    .pagination { display: flex; align-items: center; justify-content: flex-end; gap: 10px; padding: 12px 16px; color: #64748b; font-size: .78rem; }
    .pagination span:first-child { margin-right: auto; }
    .rating-only { color: #94a3b8; }
    .shop-reply { margin-top: 8px; padding: 8px 10px; background: #f0f9ff; border-left: 3px solid #0284c7; border-radius: 6px; font-size: .78rem; color: #475569; }
    .reply-toggle { font-size: .76rem !important; color: #0369a1 !important; }
    .reply-box { margin-top: 8px; }
    .reply-box textarea { width: 100%; box-sizing: border-box; padding: 8px; border: 1px solid #cbd5e1; border-radius: 8px; font: inherit; font-size: .8rem; resize: vertical; }
    .reply-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 6px; }
  `]
})
export class AdminReviewListComponent implements OnInit {
  reviews: ProductReview[] = [];
  loading = false;
  error = '';
  busyId: number | null = null;

  replyingId: number | null = null;
  replyText = '';
  replySaving = false;

  rating: number | null = null;
  status: string | null = null;
  productId: number | null = null;

  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;

  readonly ratingOptions = [5, 4, 3, 2, 1];
  readonly statusOptions = ['APPROVED', 'HIDDEN'];
  readonly stars = [1, 2, 3, 4, 5];

  constructor(
    private reviewService: ReviewService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.reviewService.getAdminReviews(
      this.page, this.size,
      this.rating ?? undefined,
      this.status ?? undefined,
      this.productId ?? undefined
    ).pipe(finalize(() => this.loading = false)).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.reviews = res.data.content;
          this.totalElements = res.data.totalElements;
          this.totalPages = res.data.totalPages;
        } else {
          this.error = res.message || 'Failed to load reviews.';
        }
      },
      error: err => this.error = err.error?.message || 'Failed to load reviews.'
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.load();
  }

  resetFilters(): void {
    this.rating = null;
    this.status = null;
    this.productId = null;
    this.page = 0;
    this.load();
  }

  changePage(delta: number): void {
    const next = this.page + delta;
    if (next < 0 || next >= this.totalPages) {
      return;
    }
    this.page = next;
    this.load();
  }

  setStatus(review: ProductReview, status: string): void {
    this.busyId = review.id;
    this.reviewService.adminUpdateStatus(review.id, status).pipe(
      finalize(() => this.busyId = null)
    ).subscribe({
      next: res => {
        if (res.success && res.data) {
          review.status = res.data.status;
          this.notification.success(status === 'HIDDEN' ? 'Review hidden.' : 'Review approved.');
        }
      },
      error: err => this.notification.error(err.error?.message || 'Failed to update review status.')
    });
  }

  startReply(review: ProductReview): void {
    this.replyingId = review.id;
    this.replyText = review.sellerReply || '';
  }

  cancelReply(): void {
    this.replyingId = null;
    this.replyText = '';
  }

  submitReply(review: ProductReview): void {
    const text = this.replyText.trim();
    if (!text) {
      return;
    }
    this.replySaving = true;
    this.reviewService.adminReply(review.id, text).pipe(
      finalize(() => this.replySaving = false)
    ).subscribe({
      next: res => {
        if (res.success && res.data) {
          review.sellerReply = res.data.sellerReply;
          this.notification.success('Reply saved.');
          this.cancelReply();
        }
      },
      error: err => this.notification.error(err.error?.message || 'Failed to save reply.')
    });
  }
}
