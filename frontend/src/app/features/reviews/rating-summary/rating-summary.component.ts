import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RatingSummary } from '../../../core/models/review.model';

@Component({
  selector: 'app-rating-summary',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <section class="summary" aria-label="Rating summary">
      <div class="score">
        <strong>{{ summary.averageRating | number:'1.1-1' }}</strong>
        <div class="stars" [attr.aria-label]="summary.averageRating + ' out of 5 stars'">
          @for (star of stars; track star) {
            <mat-icon>{{ star <= roundedRating ? 'star' : 'star_border' }}</mat-icon>
          }
        </div>
        <span>{{ summary.totalReviews }} {{ summary.totalReviews === 1 ? 'review' : 'reviews' }}</span>
      </div>
      <div class="distribution">
        @for (star of reversedStars; track star) {
          <div class="row">
            <span>{{ star }} star</span>
            <div class="bar"><i [style.width.%]="percentage(star)"></i></div>
            <span>{{ count(star) }}</span>
          </div>
        }
      </div>
    </section>
  `,
  styles: [`
    .summary { display:grid; grid-template-columns:160px 1fr; gap:28px; align-items:center; }
    .score { text-align:center; color:var(--text-muted); }
    .score strong { display:block; font-size:3rem; color:var(--text-main); line-height:1; }
    .stars { display:flex; justify-content:center; color:#f59e0b; margin:8px 0; }
    .row { display:grid; grid-template-columns:48px 1fr 32px; gap:10px; align-items:center; margin:7px 0; font-size:.82rem; }
    .bar { height:8px; overflow:hidden; border-radius:99px; background:#e2e8f0; }
    .bar i { display:block; height:100%; background:#f59e0b; border-radius:inherit; }
    @media (max-width:600px) { .summary { grid-template-columns:1fr; } }
  `]
})
export class RatingSummaryComponent {
  @Input({ required: true }) summary!: RatingSummary;
  readonly stars = [1, 2, 3, 4, 5];
  readonly reversedStars = [5, 4, 3, 2, 1];

  get roundedRating(): number {
    return Math.round(this.summary.averageRating);
  }

  count(star: number): number {
    return this.summary.starCounts?.[star] ?? 0;
  }

  percentage(star: number): number {
    return this.summary.totalReviews ? (this.count(star) / this.summary.totalReviews) * 100 : 0;
  }
}
