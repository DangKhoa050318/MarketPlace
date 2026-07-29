import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  QueryList,
  SimpleChanges,
  ViewChild,
  ViewChildren
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Subject, finalize, startWith, takeUntil } from 'rxjs';
import {
  AnalyticsEventSource,
  AnalyticsEventType,
  RecommendationPlacement
} from '../../../core/models/analytics-event.model';
import {
  RecommendationItem,
  RecommendationResponse
} from '../../../core/models/recommendation.model';
import { AnalyticsService } from '../../../core/services/analytics.service';
import {
  RecommendationAttributionService
} from '../../../core/services/recommendation-attribution.service';
import { RecommendationService } from '../../../core/services/recommendation.service';

@Component({
  selector: 'app-recommendation-carousel',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule],
  template: `
    @if (loading) {
      <section class="recommendation-block" [attr.aria-labelledby]="headingId">
        <div class="heading">
          <div>
            <span class="eyebrow">Dành cho bạn</span>
            <h2 [id]="headingId">{{ title }}</h2>
          </div>
        </div>
        <div class="carousel skeletons" aria-label="Đang tải gợi ý sản phẩm" aria-busy="true">
          @for (slot of skeletonSlots; track slot) {
            <div class="skeleton-card" aria-hidden="true">
              <div class="skeleton image"></div>
              <div class="skeleton line wide"></div>
              <div class="skeleton line"></div>
            </div>
          }
        </div>
      </section>
    } @else if (errorMessage) {
      <section class="recommendation-block error" [attr.aria-labelledby]="headingId">
        <div>
          <span class="eyebrow">Dành cho bạn</span>
          <h2 [id]="headingId">{{ title }}</h2>
          <p>{{ errorMessage }}</p>
        </div>
        <button mat-stroked-button type="button" (click)="load()">
          <mat-icon>refresh</mat-icon>
          Thử lại
        </button>
      </section>
    } @else if (items.length > 0) {
      <section
        class="recommendation-block"
        role="region"
        [attr.aria-labelledby]="headingId">
        <div class="heading">
          <div>
            <span class="eyebrow">Dành cho bạn</span>
            <h2 [id]="headingId">{{ title }}</h2>
          </div>
          <div
            class="controls"
            role="group"
            aria-label="Điều khiển danh sách gợi ý">
            <button
              mat-icon-button
              type="button"
              (click)="scroll(-1)"
              [attr.aria-label]="'Xem sản phẩm trước trong ' + title">
              <mat-icon>chevron_left</mat-icon>
            </button>
            <button
              mat-icon-button
              type="button"
              (click)="scroll(1)"
              [attr.aria-label]="'Xem sản phẩm tiếp theo trong ' + title">
              <mat-icon>chevron_right</mat-icon>
            </button>
          </div>
        </div>

        <div
          #viewport
          class="carousel"
          role="group"
          aria-roledescription="carousel"
          tabindex="0"
          [attr.aria-label]="title + '. Sử dụng phím mũi tên trái và phải để di chuyển.'"
          (keydown.arrowLeft)="scroll(-1); $event.preventDefault()"
          (keydown.arrowRight)="scroll(1); $event.preventDefault()">
          @for (item of items; track item.product.id) {
            <a
              #recommendationCard
              class="recommendation-card"
              [routerLink]="['/products', item.product.id]"
              [attr.data-position]="item.position"
              [attr.aria-label]="'Xem ' + item.product.name"
              (click)="trackClick(item)">
              <div class="image-box">
                <img
                  [src]="imageFor(item)"
                  (error)="useFallback($event)"
                  [alt]="item.product.name">
                @if (item.product.categoryName) {
                  <span class="category">{{ item.product.categoryName }}</span>
                }
              </div>
              <div class="card-body">
                <p class="product-name">{{ item.product.name }}</p>
                @if (item.product.brand) {
                  <span class="brand">{{ item.product.brand }}</span>
                }
                <div class="price-row">
                  <strong>{{ priceFor(item) | currency:'USD':'symbol':'1.0-0' }}</strong>
                  <span>Chi tiết <mat-icon>arrow_forward</mat-icon></span>
                </div>
              </div>
            </a>
          }
        </div>
        <p class="sr-only" aria-live="polite">
          Đã tải {{ items.length }} sản phẩm gợi ý.
        </p>
      </section>
    } @else {
      <p class="sr-only" aria-live="polite">
        Hiện chưa có sản phẩm gợi ý cho {{ title }}.
      </p>
    }
  `,
  styles: [`
    :host { display: block; }
    * { box-sizing: border-box; }
    .recommendation-block {
      margin: 24px 0;
      padding: 22px;
      overflow: hidden;
      border: 1px solid #e2e8f0;
      border-radius: 18px;
      background: #fff;
      box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
    }
    .heading {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 16px;
    }
    .eyebrow {
      color: #b7791f;
      font-size: .72rem;
      font-weight: 800;
      letter-spacing: .14em;
      text-transform: uppercase;
    }
    h2 {
      margin: 3px 0 0;
      color: #0f172a;
      font-size: clamp(1.2rem, 2.2vw, 1.55rem);
      font-weight: 850;
      letter-spacing: -.02em;
    }
    .controls { display: flex; gap: 6px; }
    .controls button {
      border: 1px solid #dbe4ea;
      background: #f8fafc;
      color: #0f4c5c;
    }
    .carousel {
      display: grid;
      grid-auto-flow: column;
      grid-auto-columns: minmax(210px, 23%);
      gap: 14px;
      overflow-x: auto;
      overscroll-behavior-inline: contain;
      scroll-behavior: smooth;
      scroll-snap-type: inline mandatory;
      scrollbar-width: thin;
      padding: 2px 2px 12px;
      outline: none;
    }
    .carousel:focus-visible {
      border-radius: 12px;
      outline: 3px solid rgba(2, 132, 199, .35);
      outline-offset: 4px;
    }
    .recommendation-card {
      min-width: 0;
      overflow: hidden;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      background: #fff;
      color: inherit;
      text-decoration: none;
      scroll-snap-align: start;
      transition: transform .2s ease, border-color .2s ease, box-shadow .2s ease;
    }
    .recommendation-card:hover,
    .recommendation-card:focus-visible {
      border-color: #0e7490;
      outline: none;
      transform: translateY(-3px);
      box-shadow: 0 12px 24px rgba(14, 116, 144, .14);
    }
    .image-box {
      position: relative;
      aspect-ratio: 4 / 3;
      overflow: hidden;
      background: #f1f5f9;
    }
    .image-box img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform .3s ease;
    }
    .recommendation-card:hover img { transform: scale(1.04); }
    .category {
      position: absolute;
      top: 10px;
      left: 10px;
      max-width: calc(100% - 20px);
      overflow: hidden;
      padding: 4px 8px;
      border: 1px solid rgba(226, 232, 240, .9);
      border-radius: 7px;
      background: rgba(255, 255, 255, .92);
      color: #334155;
      font-size: .68rem;
      font-weight: 750;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .card-body {
      display: flex;
      min-height: 126px;
      flex-direction: column;
      padding: 13px;
    }
    .product-name {
      display: -webkit-box;
      min-height: 2.7em;
      overflow: hidden;
      margin: 0 0 5px;
      color: #0f172a;
      font-size: .95rem;
      font-weight: 800;
      line-height: 1.35;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
    }
    .brand {
      color: #64748b;
      font-size: .72rem;
      font-weight: 700;
      text-transform: uppercase;
    }
    .price-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      margin-top: auto;
      padding-top: 12px;
    }
    .price-row strong { color: #0f4c5c; font-size: 1.05rem; }
    .price-row span {
      display: inline-flex;
      align-items: center;
      color: #0369a1;
      font-size: .72rem;
      font-weight: 750;
    }
    .price-row mat-icon { width: 15px; height: 15px; font-size: 15px; }
    .error {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
    }
    .error p { margin: 6px 0 0; color: #b91c1c; }
    .skeletons { overflow: hidden; }
    .skeleton-card {
      min-height: 260px;
      padding: 10px;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
    }
    .skeleton {
      border-radius: 8px;
      background: linear-gradient(90deg, #edf2f7 25%, #f8fafc 50%, #edf2f7 75%);
      background-size: 200% 100%;
      animation: shimmer 1.4s infinite;
    }
    .skeleton.image { width: 100%; aspect-ratio: 4 / 3; }
    .skeleton.line { width: 62%; height: 13px; margin-top: 12px; }
    .skeleton.line.wide { width: 88%; }
    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
    }
    @keyframes shimmer { to { background-position-x: -200%; } }
    @media (max-width: 980px) {
      .carousel { grid-auto-columns: minmax(210px, 42%); }
    }
    @media (max-width: 620px) {
      .recommendation-block { padding: 17px; border-radius: 15px; }
      .carousel { grid-auto-columns: minmax(210px, 82%); }
      .controls button { width: 40px; height: 40px; }
      .error { align-items: flex-start; flex-direction: column; }
    }
    @media (prefers-reduced-motion: reduce) {
      .carousel { scroll-behavior: auto; }
      .recommendation-card, .image-box img { transition: none; }
      .skeleton { animation: none; }
    }
  `]
})
export class RecommendationCarouselComponent implements OnChanges, AfterViewInit, OnDestroy {
  @Input({ required: true }) title = '';
  @Input({ required: true }) placement!: RecommendationPlacement;
  @Input() productId?: number;
  @Input() categoryId?: number;
  @Input() limit = 12;

  @ViewChild('viewport') viewport?: ElementRef<HTMLElement>;
  @ViewChildren('recommendationCard', { read: ElementRef })
  recommendationCards!: QueryList<ElementRef<HTMLElement>>;

  readonly fallbackImage =
    'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800';
  readonly skeletonSlots = [1, 2, 3, 4];
  readonly headingId = `recommendation-heading-${Math.random().toString(36).slice(2)}`;

  response?: RecommendationResponse;
  loading = false;
  errorMessage = '';

  private readonly destroy$ = new Subject<void>();
  private readonly impressedPositions = new Set<number>();
  private impressionObserver?: IntersectionObserver;
  private requestSequence = 0;

  constructor(
    private recommendationService: RecommendationService,
    private analyticsService: AnalyticsService,
    private attributionService: RecommendationAttributionService
  ) {}

  get items(): RecommendationItem[] {
    return this.response?.items ?? [];
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (
      changes['placement']
      || changes['productId']
      || changes['categoryId']
      || changes['limit']
    ) {
      this.load();
    }
  }

  ngAfterViewInit(): void {
    this.recommendationCards.changes.pipe(
      startWith(this.recommendationCards),
      takeUntil(this.destroy$)
    ).subscribe(() => this.observeCards());
  }

  ngOnDestroy(): void {
    this.impressionObserver?.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    if (!this.placement || !this.hasRequiredContext()) {
      this.response = undefined;
      this.loading = false;
      return;
    }

    const sequence = ++this.requestSequence;
    this.loading = true;
    this.errorMessage = '';
    this.response = undefined;
    this.impressedPositions.clear();
    this.impressionObserver?.disconnect();

    this.recommendationService.getRecommendations({
      placement: this.placement,
      productId: this.productId,
      categoryId: this.categoryId,
      limit: this.limit
    }).pipe(
      takeUntil(this.destroy$),
      finalize(() => {
        if (sequence === this.requestSequence) {
          this.loading = false;
        }
      })
    ).subscribe({
      next: result => {
        if (sequence !== this.requestSequence) return;
        this.response = result.success ? result.data : undefined;
        if (!result.success) {
          this.errorMessage = result.message || 'Không thể tải sản phẩm gợi ý.';
        }
      },
      error: () => {
        if (sequence === this.requestSequence) {
          this.errorMessage = 'Không thể tải sản phẩm gợi ý.';
        }
      }
    });
  }

  scroll(direction: -1 | 1): void {
    const viewport = this.viewport?.nativeElement;
    if (!viewport) return;
    viewport.scrollBy({
      left: direction * Math.max(220, viewport.clientWidth * 0.8),
      behavior: 'smooth'
    });
  }

  trackClick(item: RecommendationItem): void {
    const response = this.response;
    if (!response) return;
    const context = this.eventContext(item, response);
    this.analyticsService.track(
      AnalyticsEventType.RecommendationClick,
      { score: item.score, reason: item.reason },
      context
    );
    this.attributionService.remember({
      productId: item.product.id,
      recommendationRequestId: response.requestId,
      placement: response.placement,
      strategy: response.strategy,
      position: item.position
    });
  }

  recordImpression(position: number): void {
    if (this.impressedPositions.has(position) || !this.response) return;
    const item = this.items.find(candidate => candidate.position === position);
    if (!item) return;
    this.impressedPositions.add(position);
    this.analyticsService.track(
      AnalyticsEventType.RecommendationImpression,
      { score: item.score, reason: item.reason },
      this.eventContext(item, this.response)
    );
  }

  imageFor(item: RecommendationItem): string {
    return item.product.imageUrl
      || item.product.variants?.find(variant => variant.active && variant.imageUrl)?.imageUrl
      || this.fallbackImage;
  }

  priceFor(item: RecommendationItem): number {
    const activePrices = (item.product.variants ?? [])
      .filter(variant => variant.active)
      .map(variant => variant.price);
    return activePrices.length > 0 ? Math.min(...activePrices) : item.product.price ?? 0;
  }

  useFallback(event: Event): void {
    (event.target as HTMLImageElement).src = this.fallbackImage;
  }

  private observeCards(): void {
    if (
      typeof IntersectionObserver === 'undefined'
      || !this.recommendationCards
      || this.recommendationCards.length === 0
    ) {
      return;
    }
    this.impressionObserver?.disconnect();
    this.impressionObserver = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting || entry.intersectionRatio < 0.5) return;
        const position = Number((entry.target as HTMLElement).dataset['position']);
        if (Number.isFinite(position)) {
          this.recordImpression(position);
          this.impressionObserver?.unobserve(entry.target);
        }
      });
    }, { threshold: 0.5 });
    this.recommendationCards.forEach(card =>
      this.impressionObserver?.observe(card.nativeElement)
    );
  }

  private eventContext(
    item: RecommendationItem,
    response: RecommendationResponse
  ) {
    return {
      productId: item.product.id,
      source: AnalyticsEventSource.Recommendation,
      placement: response.placement,
      recommendationRequestId: response.requestId,
      strategy: response.strategy,
      position: item.position
    };
  }

  private hasRequiredContext(): boolean {
    switch (this.placement) {
      case RecommendationPlacement.ProductDetailSimilar:
      case RecommendationPlacement.ProductDetailCoViewed:
      case RecommendationPlacement.ProductDetailCoPurchased:
        return !!this.productId;
      case RecommendationPlacement.CategoryBestSellers:
        return !!this.categoryId;
      case RecommendationPlacement.HomeBestSellers:
        return true;
    }
  }
}
