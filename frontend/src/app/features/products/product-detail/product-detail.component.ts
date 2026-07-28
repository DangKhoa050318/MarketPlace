import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PageEvent } from '@angular/material/paginator';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { ProductResponse, ProductVariant } from '../../../core/models/product.model';
import {
  ProductReview, RatingSummary, ReviewEligibility, ReviewPayload
} from '../../../core/models/review.model';
import { ProductService } from '../../../core/services/product.service';
import { ReviewService, ReviewSort } from '../../../core/services/review.service';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { NotificationService } from '../../../core/services/notification.service';
import { RatingSummaryComponent } from '../../reviews/rating-summary/rating-summary.component';
import { ReviewFormComponent } from '../../reviews/review-form/review-form.component';
import { ReviewListComponent } from '../../reviews/review-list/review-list.component';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    RatingSummaryComponent, ReviewFormComponent, ReviewListComponent
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
          <span class="eyebrow">{{ product.categoryName }}</span>
          <h1>{{ product.name }}</h1>
          <p class="description">{{ product.description }}</p>

          <div class="price-row">
            @if (selectedVariant) {
              <span class="main-price">{{ selectedVariant.price | currency:'USD':'symbol':'1.2-2' }}</span>
              <span class="sku-badge">SKU: {{ selectedVariant.sku }}</span>
            } @else {
              <span class="main-price">{{ product.price | currency:'USD':'symbol':'1.2-2' }}</span>
            }
          </div>

          <!-- Variant Selector -->
          @if (variantsLoading) {
            <div class="subtle-loading"><mat-spinner diameter="20"></mat-spinner> <span>Đang tải các lựa chọn mẫu mã...</span></div>
          } @else if (variants.length > 0) {
            <div class="variant-section">
              <label class="field-label">Lựa chọn mẫu mã:</label>
              <div class="variant-chips">
                @for (v of variants; track v.id) {
                  <button type="button" class="variant-chip"
                          [class.active]="selectedVariantId === v.id"
                          (click)="selectVariant(v)">
                    <span>{{ v.variantName }}</span>
                    <small>{{ v.price | currency:'USD':'symbol':'1.0-0' }}</small>
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

      <section class="reviews surface-card">
        <h2>Đánh giá & Nhận xét</h2>
        @if (summaryLoading) {
          <div class="state compact"><mat-spinner diameter="30"></mat-spinner></div>
        } @else if (summaryError) {
          <div class="inline-error">{{ summaryError }} <button mat-button (click)="loadSummary()">Thử lại</button></div>
        } @else if (summary) {
          <app-rating-summary [summary]="summary"></app-rating-summary>
        }

        @if (eligibilityLoading) {
          <p>Kiểm tra quyền viết đánh giá…</p>
        } @else if (eligibility && (eligibility.eligible || editingReview)) {
          <app-review-form [review]="editingReview" [saving]="saving" [error]="saveError"
                           (save)="saveReview($event)" (cancel)="editingReview = undefined"></app-review-form>
        } @else if (eligibility?.existingReview) {
          <p class="eligibility">{{ eligibility?.message }} Bạn có thể chỉnh sửa phía dưới.</p>
        }

        <app-review-list [reviews]="reviews" [loading]="reviewsLoading" [error]="reviewsError"
                         [totalElements]="totalElements" [page]="page" [pageSize]="pageSize"
                         [editableReviewId]="eligibility?.existingReview?.id"
                         (filterChange)="changeFilter($event)" (pageChange)="changePage($event)"
                         (retry)="loadReviews()" (edit)="editingReview = $event"></app-review-list>
      </section>
    }
  `,
  styles: [`
    :host { display:block; }
    .back-link { margin-bottom: 12px; color: #475569; }
    .product,.reviews { padding:28px; margin:12px 0 24px; border-radius: 16px; }
    .product { display:grid; grid-template-columns:minmax(240px,380px) 1fr; gap:36px; background:#fff; border:1px solid #e2e8f0; }
    .product-media img { width:100%; aspect-ratio:4/3; object-fit:cover; border-radius:14px; background:#f8fafc; }
    .product-info { display:flex; flex-direction:column; }
    h1 { margin:6px 0 12px; font-size: 1.8rem; font-weight: 800; color: #0f172a; }
    h2 { margin-top:0; font-weight: 800; color: #0f172a; }
    .eyebrow { color:#0284c7; font-weight:800; font-size: 0.75rem; letter-spacing: 0.1em; text-transform: uppercase; }
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
    .eligibility { background:#eff6ff; padding:12px; border-radius:8px; }
    @media (max-width:760px) { .product { grid-template-columns:1fr; } }
  `]
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  readonly fallbackImage = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800';
  private readonly destroy$ = new Subject<void>();
  productId = 0;
  product?: ProductResponse;
  variants: ProductVariant[] = [];
  selectedVariantId?: number;
  selectedVariant?: ProductVariant;
  quantity = 1;
  variantsLoading = true;
  addingToCart = false;
  summary?: RatingSummary;
  eligibility?: ReviewEligibility;
  reviews: ProductReview[] = [];
  editingReview?: ProductReview;
  productLoading = true;
  summaryLoading = true;
  eligibilityLoading = true;
  reviewsLoading = true;
  saving = false;
  productError = '';
  summaryError = '';
  reviewsError = '';
  saveError = '';
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
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.productId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadProduct();
    this.loadVariants();
    this.loadSummary();
    this.loadReviews();
    this.loadEligibility();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadProduct(): void {
    this.productLoading = true;
    this.productError = '';
    this.productService.getProductById(this.productId).pipe(
      takeUntil(this.destroy$), finalize(() => this.productLoading = false)
    ).subscribe({
      next: response => this.product = response.data,
      error: () => this.productError = 'Không thể tải thông tin sản phẩm này.'
    });
  }

  loadVariants(): void {
    this.variantsLoading = true;
    this.productService.getVariants(this.productId).pipe(
      takeUntil(this.destroy$), finalize(() => this.variantsLoading = false)
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
      takeUntil(this.destroy$),
      finalize(() => this.addingToCart = false)
    ).subscribe({
      next: () => {
        this.notification.success(`Đã thêm ${this.quantity} “${this.product?.name}” vào giỏ hàng!`);
        if (checkout) {
          this.router.navigate(['/cart']);
        }
      },
      error: err => this.notification.error(err.error?.message || 'Không thể thêm vào giỏ hàng.')
    });
  }

  loadSummary(): void {
    this.summaryLoading = true;
    this.summaryError = '';
    this.reviewService.getSummary(this.productId).pipe(
      takeUntil(this.destroy$), finalize(() => this.summaryLoading = false)
    ).subscribe({
      next: response => this.summary = response.data,
      error: () => this.summaryError = 'Không thể tải tổng quan đánh giá.'
    });
  }

  loadReviews(): void {
    this.reviewsLoading = true;
    this.reviewsError = '';
    this.reviewService.getReviews(this.productId, this.page, this.pageSize, this.rating, this.sort).pipe(
      takeUntil(this.destroy$), finalize(() => this.reviewsLoading = false)
    ).subscribe({
      next: response => {
        this.reviews = response.data.content;
        this.totalElements = response.data.totalElements;
      },
      error: () => this.reviewsError = 'Không thể tải danh sách nhận xét.'
    });
  }

  loadEligibility(): void {
    if (!this.authService.isAuthenticated()) {
      this.eligibilityLoading = false;
      return;
    }
    this.eligibilityLoading = true;
    this.reviewService.getEligibility(this.productId).pipe(
      takeUntil(this.destroy$), finalize(() => this.eligibilityLoading = false)
    ).subscribe({ next: response => this.eligibility = response.data, error: () => undefined });
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

  saveReview(payload: ReviewPayload): void {
    this.saving = true;
    this.saveError = '';
    const request$ = this.editingReview
      ? this.reviewService.update(this.editingReview.id, payload)
      : this.reviewService.create(this.productId, payload);
    request$.pipe(
      takeUntil(this.destroy$), finalize(() => this.saving = false)
    ).subscribe({
      next: () => {
        this.editingReview = undefined;
        this.page = 0;
        this.loadSummary();
        this.loadReviews();
        this.loadEligibility();
      },
      error: error => this.saveError = error.error?.message || 'Không thể lưu nhận xét của bạn.'
    });
  }
}
