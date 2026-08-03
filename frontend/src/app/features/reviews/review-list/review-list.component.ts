import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { ProductReview } from '../../../core/models/review.model';
import { ReviewSort } from '../../../core/services/review.service';
import { VoteService } from '../../../core/services/vote.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-review-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule, MatPaginatorModule,
    MatProgressSpinnerModule, MatSelectModule, MatFormFieldModule
  ],
  template: `
    <section>
      <div class="toolbar">
        <h3>Customer reviews</h3>
        <div class="filter-controls">
          <mat-form-field appearance="outline" class="filter-select">
            <mat-label>Rating</mat-label>
            <mat-select aria-label="Filter by stars" [(ngModel)]="selectedRating" (selectionChange)="filtersChanged()">
              <mat-option value="ALL">All ratings</mat-option>
              <mat-option *ngFor="let star of stars" [value]="star.toString()">{{ star }} ⭐</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="filter-select">
            <mat-label>Sort by</mat-label>
            <mat-select aria-label="Sort reviews" [(ngModel)]="sort" (selectionChange)="filtersChanged()">
              <mat-option value="newest">Newest</mat-option>
              <mat-option value="helpful">Most helpful</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      </div>

      @if (loading) {
        <div class="state"><mat-spinner diameter="36"></mat-spinner><span>Loading reviews…</span></div>
      } @else if (error) {
        <div class="state error" role="alert">
          <mat-icon>error_outline</mat-icon><span>{{ error }}</span>
          <button mat-stroked-button (click)="retry.emit()">Retry</button>
        </div>
      } @else {
        @for (review of reviews; track review.id) {
          <article class="review">
            <div class="review-head">
              <div>
                <strong>{{ review.userFullName || review.username }}</strong>
                <div class="stars" [attr.aria-label]="review.rating + ' out of 5 stars'">
                  @for (star of stars; track star) {
                    <mat-icon>{{ star <= review.rating ? 'star' : 'star_border' }}</mat-icon>
                  }
                </div>
              </div>
              <time [attr.datetime]="review.createdAt">{{ review.createdAt | date:'mediumDate' }}</time>
            </div>
            <div class="badges">
              @if (review.isVerifiedPurchase) {
                <span class="verified"><mat-icon>verified</mat-icon>Verified Purchase</span>
              }
              @if (review.isEdited) { <span>Edited</span> }
            </div>
            <h4>{{ review.title }}</h4>
            <p>{{ review.content }}</p>
            @if (review.imageUrl) {
              <div class="review-image-container">
                <img [src]="review.imageUrl" [alt]="review.title" class="review-img">
              </div>
            }
            <div class="review-actions">
              <button
                mat-button
                class="vote-btn sm"
                [class.voted]="review.isVotedByCurrentUser"
                (click)="vote(review)">
                <mat-icon>{{ review.isVotedByCurrentUser ? 'thumb_up' : 'thumb_up_off_alt' }}</mat-icon>
                <span>Hữu ích ({{ review.helpfulCount || 0 }})</span>
              </button>
              @if (review.id === editableReviewId) {
                <button mat-button color="primary" (click)="edit.emit(review)">Edit review</button>
              }
            </div>
          </article>
        } @empty {
          <div class="state"><mat-icon>rate_review</mat-icon><span>No reviews match this filter yet.</span></div>
        }
        @if (totalElements > pageSize) {
          <mat-paginator [length]="totalElements" [pageIndex]="page" [pageSize]="pageSize"
                         [hidePageSize]="true" (page)="pageChange.emit($event)"></mat-paginator>
        }
      }
    </section>
  `,
  styles: [`
    .toolbar,.review-head { display:flex; justify-content:space-between; align-items:center; gap:16px; }
    .filter-controls { display:flex; gap:12px; align-items:center; }
    .filter-select { width:150px; margin-bottom:-1.25em; font-size:0.85rem; }
    .review { border-top:1px solid #e2e8f0; padding:20px 0; }
    .review h4 { margin:10px 0 4px; } .review p { margin:0; white-space:pre-wrap; }
    .stars { display:flex; color:#f59e0b; } .stars mat-icon { font-size:18px; width:18px; height:18px; }
    time,.badges { color:var(--text-muted); font-size:.8rem; }
    .badges { display:flex; gap:10px; margin-top:8px; }
    .verified { display:inline-flex; align-items:center; color:#047857; font-weight:700; }
    .verified mat-icon { font-size:16px; width:16px; height:16px; margin-right:3px; }
    .review-image-container { margin-top: 10px; }
    .review-img { max-width: 160px; max-height: 160px; object-fit: cover; border-radius: 10px; border: 1px solid #cbd5e1; cursor: pointer; transition: transform 0.2s ease; }
    .review-img:hover { transform: scale(1.03); }
    .review-actions { display:flex; justify-content:space-between; align-items:center; margin-top:12px; }
    .vote-btn { border-radius:16px; font-size:.82rem; color:#64748b; }
    .vote-btn.voted { color:#0284c7; background:#f0f9ff; }
    .state { min-height:150px; display:flex; align-items:center; justify-content:center; flex-direction:column; gap:10px; color:var(--text-muted); }
    .error { color:#b91c1c; }
    @media (max-width:600px) { .toolbar { align-items:flex-start; flex-direction:column; } }
  `]
})
export class ReviewListComponent {
  @Input() reviews: (ProductReview & { isVotedByCurrentUser?: boolean })[] = [];
  @Input() loading = false;
  @Input() error = '';
  @Input() totalElements = 0;
  @Input() page = 0;
  @Input() pageSize = 5;
  @Input() editableReviewId?: number;
  @Output() filterChange = new EventEmitter<{ rating?: number; sort: ReviewSort }>();
  @Output() pageChange = new EventEmitter<PageEvent>();
  @Output() retry = new EventEmitter<void>();
  @Output() edit = new EventEmitter<ProductReview>();

  selectedRating = 'ALL';
  sort: ReviewSort = 'newest';
  readonly stars = [1, 2, 3, 4, 5];

  constructor(
    private voteService: VoteService,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  filtersChanged(): void {
    const ratingVal = this.selectedRating !== 'ALL' ? Number(this.selectedRating) : undefined;
    this.filterChange.emit({ rating: ratingVal, sort: this.sort });
  }

  vote(review: ProductReview & { isVotedByCurrentUser?: boolean }): void {
    if (!this.authService.isAuthenticated()) {
      this.notificationService.info('Vui lòng đăng nhập để bình chọn');
      return;
    }

    this.voteService.toggleVote({ targetType: 'REVIEW', targetId: review.id }).subscribe({
      next: (res) => {
        if (res.data) {
          review.isVotedByCurrentUser = res.data.isVoted;
          review.helpfulCount = res.data.helpfulCount;
        }
      },
      error: () => {
        this.notificationService.error('Không thể thực hiện bình chọn');
      }
    });
  }
}
