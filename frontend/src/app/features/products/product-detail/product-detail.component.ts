import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PageEvent } from '@angular/material/paginator';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import {
  AnalyticsEventContext,
  AnalyticsEventSource,
  AnalyticsEventType,
  RecommendationPlacement
} from '../../../core/models/analytics-event.model';
import { ProductResponse, ProductVariant } from '../../../core/models/product.model';
import {
  ProductReview, RatingSummary
} from '../../../core/models/review.model';
import { ProductService } from '../../../core/services/product.service';
import { ReviewService, ReviewSort } from '../../../core/services/review.service';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AnalyticsService } from '../../../core/services/analytics.service';
import {
  RecommendationAttributionService
} from '../../../core/services/recommendation-attribution.service';
import { RecentlyViewedService } from '../../../core/services/recently-viewed.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import {
  RecommendationCarouselComponent
} from '../../../shared/components/recommendation-carousel/recommendation-carousel.component';
import { RatingSummaryComponent } from '../../reviews/rating-summary/rating-summary.component';
import { ReviewListComponent } from '../../reviews/review-list/review-list.component';
import { ProductQuestionListComponent } from '../../questions/product-question-list/product-question-list.component';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    RatingSummaryComponent, ReviewListComponent, ProductQuestionListComponent, RecommendationCarouselComponent
  ],
  template: `
    <a mat-button routerLink="/products" class="back-link"><mat-icon>arrow_back</mat-icon>Sản phẩm</a>
    @if (productLoading) {
      <div class="state"><mat-spinner diameter="44"></mat-spinner></div>
    } @else if (productError) {
      <div class="state error"><p>{{ productError }}</p><button mat-stroked-button (click)="loadProduct()">Thử lại</button></div>
    } @else if (product) {
      <article class="product surface-card">
        <div class="product-media">
          <img [src]="(selectedVariant?.imageUrl || product.imageUrl) || fallbackImage" [alt]="product.name">
        </div>
        <div class="product-info">
          <div class="title-row">
            <div>
              <span class="eyebrow">{{ product.categoryName }}</span>
              <h1>{{ product.name }}</h1>
            </div>
            <button
              mat-icon-button
              type="button"
              class="wishlist-btn"
              [class.active]="wishlisted"
              [disabled]="wishlistBusy"
              (click)="toggleWishlist()"
              [attr.aria-label]="wishlisted ? 'Remove from wishlist' : 'Add to wishlist'">
              <mat-icon>{{ wishlisted ? 'favorite' : 'favorite_border' }}</mat-icon>
            </button>
          </div>
          
          <p class="description">{{ product.description }}</p>

          <div class="price-row">
            @if (selectedVariant) {
              <span class="main-price">{{ selectedVariant.price | currency:'VND':'symbol':'1.0-0' }}</span>
              <span class="sku-badge">SKU: {{ selectedVariant.sku }}</span>
            } @else {
              <span class="main-price">{{ product.price | currency:'VND':'symbol':'1.0-0' }}</span>
            }
          </div>

          <!-- Variant Selector -->
          @if (variantsLoading) {
            <div class="subtle-loading"><mat-spinner diameter="20"></mat-spinner> <span>Đang tải các lựa chọn mẫu mã...</span></div>
          } @else if (variants.length > 0) {
            <div class="variant-section">
              <span class="field-label">Lựa chọn mẫu mã:</span>
              <div class="variant-chips">
                @for (v of variants; track v.id) {
                  <button type="button" class="variant-chip"
                          [class.active]="selectedVariantId === v.id"
                          (click)="selectVariant(v)">
                    <span>{{ v.variantName }}</span>
                    <small>{{ v.price | currency:'VND':'symbol':'1.0-0' }}</small>
                  </button>
                }
              </div>
            </div>
          }

          <!-- Quantity Stepper & Actions -->
          <div class="purchase-box">
            <div class="quantity-stepper">
              <button mat-icon-button type="button" (click)="adjustQuantity(-1)" [disabled]="quantity <= 1 || addingToCart">
                <mat-icon>remove</mat-icon>
              </button>
              <span class="qty-val">{{ quantity }}</span>
              <button mat-icon-button type="button" (click)="adjustQuantity(1)" [disabled]="addingToCart">
                <mat-icon>add</mat-icon>
              </button>
            </div>

            <button mat-flat-button class="add-cart-btn" (click)="onAddToCart(false)" [disabled]="addingToCart || !selectedVariant">
              <mat-icon>{{ addingToCart ? 'hourglass_top' : 'add_shopping_cart' }}</mat-icon>
              {{ addingToCart ? 'Đang thêm...' : 'Thêm vào giỏ' }}
            </button>

            <button mat-stroked-button class="buy-now-btn" (click)="onAddToCart(true)" [disabled]="addingToCart || !selectedVariant">
              <mat-icon>bolt</mat-icon>
              Mua ngay
            </button>
          </div>
        </div>
      </article>

      <app-recommendation-carousel
        title="Sản phẩm tương tự"
        [placement]="recommendationPlacement.ProductDetailSimilar"
        [productId]="productId"
        [limit]="8">
      </app-recommendation-carousel>

      <app-recommendation-carousel
        title="Khách hàng cũng thường xem"
        [placement]="recommendationPlacement.ProductDetailCoViewed"
        [productId]="productId"
        [limit]="8">
      </app-recommendation-carousel>

      <section class="reviews surface-card" id="reviews-section">
        <h2>Đánh giá & Nhận xét</h2>
        @if (summaryLoading) {
          <div class="state compact"><mat-spinner diameter="30"></mat-spinner></div>
        } @else if (summaryError) {
          <div class="inline-error">{{ summaryError }} <button mat-button (click)="loadSummary()">Thử lại</button></div>
        } @else if (summary) {
          <app-rating-summary [summary]="summary"></app-rating-summary>
        }

        <app-review-list [reviews]="reviews" [loading]="reviewsLoading" [error]="reviewsError"
                         [totalElements]="totalElements" [page]="page" [pageSize]="pageSize"
                         (filterChange)="changeFilter($event)" (pageChange)="changePage($event)"
                         (retry)="loadReviews()"></app-review-list>
      </section>

      <app-product-question-list [productId]="product.id"></app-product-question-list>
    }
  `,
  styles: [`
    :host { display:block; }
    .back-link { margin-bottom: 12px; color: #475569; }
    .product,.reviews { padding:28px; margin:12px 0 24px; border-radius: 16px; }
    .product { display:grid; grid-template-columns:minmax(240px,380px) 1fr; gap:36px; background:#fff; border:1px solid #e2e8f0; }
    .product-media img { width:100%; aspect-ratio:4/3; object-fit:cover; border-radius:14px; background:#f8fafc; }
    .product-info { display:flex; flex-direction:column; }
    .title-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
    h1 { margin:6px 0 12px; font-size: 1.8rem; font-weight: 800; color: #0f172a; }
    h2 { margin-top:0; font-weight: 800; color: #0f172a; }
    .eyebrow { color:#0284c7; font-weight:800; font-size: 0.75rem; letter-spacing: 0.1em; text-transform: uppercase; }
    .wishlist-btn { flex: 0 0 auto; background: #fff; color: #64748b; box-shadow: 0 2px 8px rgba(15, 23, 42, 0.12); }
    .wishlist-btn.active { color: #e11d48; background: #fff1f2; }
    .description { color: #475569; font-size: 0.95rem; line-height: 1.6; margin-bottom: 20px; }
    .price-row { display: flex; align-items: baseline; gap: 14px; margin-bottom: 22px; }
    .main-price { font-size: 2rem; font-weight: 900; color: #0369a1; }
    .sku-badge { font-size: 0.8rem; background: #f1f5f9; color: #64748b; padding: 4px 10px; border-radius: 6px; font-weight: 600; }
    .field-label { display: block; font-size: 0.82rem; font-weight: 700; color: #334155; margin-bottom: 8px; }
    .variant-section { margin-bottom: 24px; }
    .variant-chips { display: flex; flex-wrap: wrap; gap: 10px; }
    .variant-chip {
      display: flex; flex-direction: column; align-items: flex-start; padding: 8px 16px;
      border: 1.5px solid #cbd5e1; border-radius: 10px; background: #fff; cursor: pointer;
      transition: all 0.2s ease; text-align: left;
    }
    .variant-chip span { font-size: 0.88rem; font-weight: 700; color: #1e293b; }
    .variant-chip small { font-size: 0.75rem; color: #64748b; margin-top: 2px; }
    .variant-chip:hover { border-color: #0284c7; background: #f0f9ff; }
    .variant-chip.active { border-color: #0284c7; background: #e0f2fe; box-shadow: 0 0 0 1px #0284c7; }
    .subtle-loading { display: flex; align-items: center; gap: 10px; color: #64748b; font-size: 0.88rem; margin-bottom: 20px; }
    .purchase-box { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; margin-top: auto; padding-top: 16px; border-top: 1px solid #f1f5f9; }
    .quantity-stepper {
      display: inline-flex; align-items: center; gap: 6px; padding: 4px 8px;
      border-radius: 10px; background: #f8fafc; border: 1px solid #cbd5e1;
    }
    .qty-val { min-width: 32px; text-align: center; font-weight: 800; font-size: 1rem; color: #0f172a; }
    .add-cart-btn { background: #0284c7!important; color: #fff!important; height: 44px; padding: 0 22px; font-weight: 700; }
    .buy-now-btn { border-color: #0284c7!important; color: #0284c7!important; height: 44px; padding: 0 20px; font-weight: 700; }
    .reviews { display:flex; flex-direction:column; gap:22px; background: #fff; border: 1px solid #e2e8f0; }
    .state { min-height:300px; display:flex; align-items:center; justify-content:center; flex-direction:column; }
    .compact { min-height:100px; } .error,.inline-error { color:#b91c1c; }
    @media (max-width:760px) { .product { grid-template-columns:1fr; } }
  `]
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  readonly fallbackImage = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800';
  readonly recommendationPlacement = RecommendationPlacement;
  private readonly destroy$ = new Subject<void>();
  private readonly productChange$ = new Subject<void>();
  private productViewAnalyticsContext: AnalyticsEventContext = {
    source: AnalyticsEventSource.Direct
  };
  productId = 0;
  product?: ProductResponse;
  variants: ProductVariant[] = [];
  selectedVariantId?: number;
  selectedVariant?: ProductVariant;
  quantity = 1;
  variantsLoading = true;
  addingToCart = false;
  wishlisted = false;
  wishlistBusy = false;
  summary?: RatingSummary;
  reviews: ProductReview[] = [];
  productLoading = true;
  summaryLoading = true;
  reviewsLoading = true;
  productError = '';
  summaryError = '';
  reviewsError = '';
  totalElements = 0;
  page = 0;
  pageSize = 5;
  rating?: number;
  sort: ReviewSort = 'newest';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    private reviewService: ReviewService,
    private authService: AuthService,
    private cartService: CartService,
    private notification: NotificationService,
    private analyticsService: AnalyticsService,
    private recommendationAttributionService: RecommendationAttributionService,
    private recentlyViewedService: RecentlyViewedService,
    private wishlistService: WishlistService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const productId = Number(params.get('id'));
      if (!productId || productId === this.productId) return;
      this.productChange$.next();
      this.productId = productId;
      const recommendationContext =
        this.recommendationAttributionService.consumeProductViewContext(productId);
      if (recommendationContext) {
        this.productViewAnalyticsContext = recommendationContext;
      } else {
        this.recommendationAttributionService.clear(productId);
        this.productViewAnalyticsContext = {
          productId,
          source: AnalyticsEventSource.Direct
        };
      }
      this.resetProductState();
      this.loadProduct();
      this.loadVariants();
      this.loadSummary();
      this.loadReviews();
      this.loadWishlistStatus(productId);
      this.recordRecentlyViewed(productId);

      const writeReview = this.route.snapshot?.queryParamMap?.get('writeReview');
      const fragment = this.route.snapshot?.fragment;
      if (writeReview === 'true' || fragment === 'reviews') {
        setTimeout(() => {
          const el = document.getElementById('reviews-section');
          if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'start' });
          }
        }, 500);
      }
    });
  }

  ngOnDestroy(): void {
    this.productChange$.next();
    this.productChange$.complete();
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadProduct(): void {
    this.productLoading = true;
    this.productError = '';
    this.productService.getProductById(this.productId).pipe(
      takeUntil(this.productChange$),
      takeUntil(this.destroy$),
      finalize(() => this.productLoading = false)
    ).subscribe({
      next: response => {
        this.product = response.data;
        if (this.product) {
          this.analyticsService.track(AnalyticsEventType.ProductView, {
            productName: this.product.name,
            categoryId: this.product.categoryId,
            categoryName: this.product.categoryName
          }, this.productViewAnalyticsContext);
        }
      },
      error: () => this.productError = 'Không thể tải thông tin sản phẩm này.'
    });
  }

  loadVariants(): void {
    this.variantsLoading = true;
    this.productService.getVariants(this.productId).pipe(
      takeUntil(this.productChange$),
      takeUntil(this.destroy$),
      finalize(() => this.variantsLoading = false)
    ).subscribe({
      next: response => {
        this.variants = (response.data || []).filter(v => v.active);
        if (this.variants.length > 0) {
          this.selectedVariant = this.variants[0];
          this.selectedVariantId = this.variants[0].id;
        }
      },
      error: () => undefined
    });
  }

  selectVariant(variant: ProductVariant): void {
    this.selectedVariant = variant;
    this.selectedVariantId = variant.id;
  }

  adjustQuantity(delta: number): void {
    this.quantity = Math.max(1, this.quantity + delta);
  }

  onAddToCart(checkout: boolean): void {
    if (!this.authService.isAuthenticated()) {
      this.notification.error('Vui lòng đăng nhập để thực hiện thêm vào giỏ hàng.');
      this.router.navigate(['/login']);
      return;
    }
    if (!this.selectedVariant) {
      this.notification.error('Vui lòng chọn tùy chọn sản phẩm khả dụng.');
      return;
    }
    this.addingToCart = true;
    this.cartService.addToCart(this.selectedVariant.id, this.quantity).pipe(
      takeUntil(this.productChange$),
      takeUntil(this.destroy$),
      finalize(() => this.addingToCart = false)
    ).subscribe({
      next: () => {
        this.analyticsService.track(AnalyticsEventType.AddToCart, {
          productName: this.product?.name,
        }, {
          ...this.analyticsContext(),
          variantId: this.selectedVariant?.id,
          quantity: this.quantity
        });
        this.notification.success(`Đã thêm ${this.quantity} “${this.product?.name}” vào giỏ hàng!`);
        if (checkout) {
          this.router.navigate(['/cart']);
        }
      },
      error: err => this.notification.error(err.error?.message || 'Không thể thêm vào giỏ hàng.')
    });
  }

  toggleWishlist(): void {
    if (!this.product || this.wishlistBusy) return;

    const wasWishlisted = this.wishlisted;
    this.wishlistBusy = true;
    this.wishlisted = !wasWishlisted;

    const observer = {
      next: () => {
        this.wishlistBusy = false;
        if (!wasWishlisted) {
          this.analyticsService.track(AnalyticsEventType.AddToWishlist, {
            productName: this.product?.name
          }, this.analyticsContext());
        }
        this.notification.success(wasWishlisted ? 'Đã xóa khỏi yêu thích' : 'Đã thêm vào yêu thích');
      },
      error: (err: HttpErrorResponse) => {
        this.wishlisted = wasWishlisted;
        this.wishlistBusy = false;
        this.notification.error(err.error?.message || 'Không thể cập nhật danh sách yêu thích');
      }
    };

    if (wasWishlisted) {
      this.wishlistService.remove(this.product.id).pipe(
        takeUntil(this.productChange$),
        takeUntil(this.destroy$)
      ).subscribe(observer);
    } else {
      this.wishlistService.add(this.product.id).pipe(
        takeUntil(this.productChange$),
        takeUntil(this.destroy$)
      ).subscribe(observer);
    }
  }

  loadSummary(): void {
    this.summaryLoading = true;
    this.summaryError = '';
    this.reviewService.getSummary(this.productId).pipe(
      takeUntil(this.productChange$),
      takeUntil(this.destroy$),
      finalize(() => this.summaryLoading = false)
    ).subscribe({
      next: response => this.summary = response.data,
      error: () => this.summaryError = 'Không thể tải tổng quan đánh giá.'
    });
  }

  loadReviews(): void {
    this.reviewsLoading = true;
    this.reviewsError = '';
    this.reviewService.getReviews(this.productId, this.page, this.pageSize, this.rating, this.sort).pipe(
      takeUntil(this.productChange$),
      takeUntil(this.destroy$),
      finalize(() => this.reviewsLoading = false)
    ).subscribe({
      next: response => {
        this.reviews = response.data.content;
        this.totalElements = response.data.totalElements;
      },
      error: () => this.reviewsError = 'Không thể tải danh sách nhận xét.'
    });
  }

  changeFilter(value: { rating?: number; sort: ReviewSort }): void {
    this.rating = value.rating;
    this.sort = value.sort;
    this.page = 0;
    this.loadReviews();
  }

  changePage(event: PageEvent): void {
    this.page = event.pageIndex;
    this.loadReviews();
  }

  private loadWishlistStatus(productId: number): void {
    this.wishlistService.status(productId).pipe(
      takeUntil(this.productChange$),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (res) => this.wishlisted = !!res.data?.wishlisted,
      error: () => this.wishlisted = false
    });
  }

  private recordRecentlyViewed(productId: number): void {
    this.recentlyViewedService.record(productId).pipe(
      takeUntil(this.productChange$),
      takeUntil(this.destroy$)
    ).subscribe({
      error: () => {}
    });
  }

  private analyticsContext() {
    return this.recommendationAttributionService.contextFor(this.productId) ?? {
      productId: this.productId,
      source: AnalyticsEventSource.Direct
    };
  }

  private resetProductState(): void {
    this.product = undefined;
    this.variants = [];
    this.selectedVariant = undefined;
    this.selectedVariantId = undefined;
    this.quantity = 1;
    this.wishlisted = false;
    this.wishlistBusy = false;
    this.summary = undefined;
    this.reviews = [];
    this.productLoading = true;
    this.variantsLoading = true;
    this.summaryLoading = true;
    this.reviewsLoading = true;
    this.productError = '';
    this.summaryError = '';
    this.reviewsError = '';
    this.totalElements = 0;
    this.page = 0;
  }
}
