import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AnalyticsEventType } from '../../../core/models/analytics-event.model';
import { ProductResponse } from '../../../core/models/product.model';
import { CartService } from '../../../core/services/cart.service';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductService } from '../../../core/services/product.service';
import { RecentlyViewedService } from '../../../core/services/recently-viewed.service';
import { WishlistService } from '../../../core/services/wishlist.service';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="product-detail-page">
      <a mat-button routerLink="/products" class="back-link">
        <mat-icon>arrow_back</mat-icon>
        Back to catalog
      </a>

      @if (loading) {
        <div class="loading-container">
          <mat-spinner diameter="44"></mat-spinner>
        </div>
      } @else if (product) {
        <section class="detail-layout">
          <div class="image-panel surface-card">
            <img
              [src]="product.imageUrl || fallbackImage"
              (error)="onImageError($event)"
              [alt]="product.name"
              class="product-image"
            />
            <span class="category-badge">{{ product.categoryName || 'General' }}</span>
          </div>

          <div class="info-panel">
            <div class="title-row">
              <div>
                <p class="product-slug">{{ product.slug }}</p>
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

            <p class="description">{{ product.description || 'No description available for this product yet.' }}</p>

            <div class="meta-row">
              <div>
                <span>Price</span>
                <strong>\${{ getProductPrice(product) | number:'1.2-2' }}</strong>
              </div>
              <div>
                <span>Stock</span>
                <strong [class.out-of-stock]="getProductStock(product) <= 0">
                  {{ getProductStock(product) > 0 ? 'In Stock' : 'Out of Stock' }}
                </strong>
              </div>
              <div>
                <span>Unit</span>
                <strong>{{ product.unit || 'Each' }}</strong>
              </div>
            </div>

            @if (product.variants?.length) {
              <div class="variant-list">
                <h2>Available Variants</h2>
                @for (variant of product.variants; track variant.id) {
                  <div class="variant-row">
                    <span>{{ variant.variantName || variant.sku }}</span>
                    <strong>\${{ variant.price | number:'1.2-2' }}</strong>
                  </div>
                }
              </div>
            }

            <button
              mat-raised-button
              class="btn-solid-primary add-cart-btn"
              [disabled]="!product.active"
              (click)="onAddToCart()">
              <mat-icon>shopping_bag</mat-icon>
              Add to Cart
            </button>
          </div>
        </section>
      } @else {
        <div class="empty-state surface-card">
          <mat-icon>inventory_2</mat-icon>
          <h2>Product not found</h2>
          <a mat-button routerLink="/products">Return to catalog</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .product-detail-page {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .back-link {
      align-self: flex-start;
    }

    .detail-layout {
      display: grid;
      grid-template-columns: minmax(280px, 0.95fr) minmax(320px, 1.05fr);
      gap: 28px;
      align-items: start;
    }

    .image-panel {
      position: relative;
      overflow: hidden;
      padding: 0;
      min-height: 420px;
    }

    .product-image {
      width: 100%;
      height: 100%;
      min-height: 420px;
      object-fit: cover;
      display: block;
    }

    .category-badge {
      position: absolute;
      top: 16px;
      right: 16px;
      border-radius: 8px;
      padding: 6px 12px;
      background: #0284c7;
      color: #fff;
      font-weight: 700;
      font-size: 0.78rem;
    }

    .info-panel {
      display: flex;
      flex-direction: column;
      gap: 20px;
      padding: 8px 0;
    }

    .title-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 16px;
    }

    .product-slug {
      margin: 0 0 6px;
      color: var(--text-muted);
      font-size: 0.85rem;
    }

    h1 {
      margin: 0;
      color: var(--text-main);
      font-size: 2.35rem;
      line-height: 1.08;
      letter-spacing: 0;
    }

    .wishlist-btn {
      flex: 0 0 auto;
      background: #fff;
      color: #64748b;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.12);
    }

    .wishlist-btn.active {
      color: #e11d48;
      background: #fff1f2;
    }

    .description {
      margin: 0;
      color: var(--text-muted);
      line-height: 1.7;
      font-size: 1rem;
    }

    .meta-row {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 12px;
    }

    .meta-row div,
    .variant-row {
      border: 1px solid rgba(148, 163, 184, 0.28);
      border-radius: 8px;
      background: #fff;
      padding: 14px;
    }

    .meta-row span {
      display: block;
      color: var(--text-muted);
      font-size: 0.78rem;
      margin-bottom: 6px;
    }

    .meta-row strong {
      color: var(--text-main);
      font-size: 1rem;
    }

    .meta-row strong.out-of-stock {
      color: #ef4444;
    }

    .variant-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .variant-list h2 {
      margin: 0;
      font-size: 1rem;
      color: var(--text-main);
    }

    .variant-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
    }

    .add-cart-btn {
      width: min(100%, 280px);
      height: 44px;
    }

    .loading-container,
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      min-height: 320px;
      text-align: center;
    }

    @media (max-width: 840px) {
      .detail-layout {
        grid-template-columns: 1fr;
      }

      .meta-row {
        grid-template-columns: 1fr;
      }

      h1 {
        font-size: 1.9rem;
      }
    }
  `]
})
export class ProductDetailComponent implements OnInit {
  product: ProductResponse | null = null;
  loading = false;
  wishlisted = false;
  wishlistBusy = false;
  fallbackImage = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=900';

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private cartService: CartService,
    private analyticsService: AnalyticsService,
    private recentlyViewedService: RecentlyViewedService,
    private wishlistService: WishlistService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    const productId = Number(this.route.snapshot.paramMap.get('id'));
    if (!productId) {
      this.product = null;
      return;
    }

    this.loadProduct(productId);
    this.loadWishlistStatus(productId);
  }

  onAddToCart(): void {
    if (!this.product) return;

    const variant = this.product.variants?.[0];
    if (!variant) {
      this.notification.error('No variants available for this product');
      return;
    }

    this.cartService.addToCart(variant.id, 1).subscribe({
      next: () => {
        this.analyticsService.track(AnalyticsEventType.AddToCart, {
          productId: this.product?.id,
          productName: this.product?.name,
          variantId: variant.id,
          quantity: 1
        });
        this.notification.success(`Added "${this.product?.name}" to cart!`);
      },
      error: (err) => this.notification.error(err.error?.message || 'Failed to add item to cart')
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
            productId: this.product?.id,
            productName: this.product?.name
          });
        }
        this.notification.success(wasWishlisted ? 'Removed from wishlist' : 'Added to wishlist');
      },
      error: (err: HttpErrorResponse) => {
        this.wishlisted = wasWishlisted;
        this.wishlistBusy = false;
        this.notification.error(err.error?.message || 'Failed to update wishlist');
      }
    };

    if (wasWishlisted) {
      this.wishlistService.remove(this.product.id).subscribe(observer);
    } else {
      this.wishlistService.add(this.product.id).subscribe(observer);
    }
  }

  onImageError(event: Event): void {
    (event.target as HTMLImageElement).src = this.fallbackImage;
  }

  getProductPrice(product: ProductResponse): number {
    if (product.price !== undefined && product.price !== null) return product.price;
    if (product.variants && product.variants.length > 0) return product.variants[0].price;
    return 0;
  }

  getProductStock(product: ProductResponse): number {
    if (product.stock !== undefined && product.stock !== null) return product.stock;
    if (product.variants && product.variants.length > 0) return 10;
    return 1;
  }

  private loadProduct(productId: number): void {
    this.loading = true;
    this.productService.getProductById(productId).subscribe({
      next: (res) => {
        this.loading = false;
        this.product = res.success ? res.data : null;
        if (this.product) {
          this.recordRecentlyViewed(this.product.id);
          this.analyticsService.track(AnalyticsEventType.ProductView, {
            productId: this.product.id,
            productName: this.product.name,
            categoryId: this.product.categoryId,
            categoryName: this.product.categoryName
          });
        }
      },
      error: () => {
        this.loading = false;
        this.product = null;
        this.notification.error('Failed to load product details');
      }
    });
  }

  private loadWishlistStatus(productId: number): void {
    this.wishlistService.status(productId).subscribe({
      next: (res) => this.wishlisted = !!res.data?.wishlisted,
      error: () => this.wishlisted = false
    });
  }

  private recordRecentlyViewed(productId: number): void {
    this.recentlyViewedService.record(productId).subscribe({
      error: () => {
        // Viewing history is helpful but should never block product browsing.
      }
    });
  }
}
