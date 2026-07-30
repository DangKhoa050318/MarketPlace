import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { finalize } from 'rxjs';
import { PageResponse } from '../../core/models/api-response.model';
import { WishlistItemResponse } from '../../core/models/wishlist.model';
import { CartService } from '../../core/services/cart.service';
import { NotificationService } from '../../core/services/notification.service';
import { WishlistService } from '../../core/services/wishlist.service';

@Component({
  selector: 'app-wishlist',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule, MatPaginatorModule],
  template: `
    <section class="wishlist-page">
      <header class="page-header">
        <div>
          <span class="eyebrow">Saved items</span>
          <h1>Wishlist</h1>
        </div>
        <a mat-stroked-button routerLink="/products">
          <mat-icon>storefront</mat-icon>
          Continue shopping
        </a>
      </header>

      @if (loading) {
        <div class="state">
          <mat-icon>hourglass_top</mat-icon>
          <span>Loading wishlist...</span>
        </div>
      } @else if (!items.length) {
        <div class="state">
          <mat-icon>favorite_border</mat-icon>
          <h2>No saved products yet</h2>
          <a mat-flat-button routerLink="/products">Browse catalog</a>
        </div>
      } @else {
        <div class="wishlist-grid">
          @for (item of items; track item.id) {
            <article class="wishlist-card">
              <a [routerLink]="['/products', item.product.id]" class="image-link">
                <img [src]="item.product.imageUrl || fallbackImage" [alt]="item.product.name" (error)="useFallback($event)">
              </a>
              <div class="card-copy">
                <span>{{ item.product.categoryName || 'Catalog' }}</span>
                <a [routerLink]="['/products', item.product.id]">{{ item.product.name }}</a>
                <strong>{{ item.product.price | currency:'USD':'symbol':'1.0-0' }}</strong>
              </div>
              <div class="card-actions">
                <button mat-flat-button type="button" (click)="moveToCart(item)" [disabled]="busyIds.has(item.id)">
                  <mat-icon>shopping_cart</mat-icon>
                  Move to cart
                </button>
                <button mat-icon-button type="button" (click)="remove(item)" [disabled]="busyIds.has(item.id)" aria-label="Remove">
                  <mat-icon>delete</mat-icon>
                </button>
              </div>
            </article>
          }
        </div>
        <mat-paginator
          [length]="pageData?.totalElements || 0"
          [pageIndex]="page"
          [pageSize]="size"
          [pageSizeOptions]="[6, 12, 24]"
          (page)="changePage($event)">
        </mat-paginator>
      }
    </section>
  `,
  styles: [`
    .wishlist-page { display: grid; gap: 22px; }
    .page-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
    .eyebrow { color: #0284c7; font-size: .72rem; font-weight: 800; text-transform: uppercase; }
    h1 { margin: 4px 0 0; font-size: 2rem; color: #0f172a; }
    .wishlist-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
    .wishlist-card { display: grid; gap: 12px; padding: 14px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; }
    .image-link { display: block; aspect-ratio: 4 / 3; overflow: hidden; border-radius: 8px; background: #f1f5f9; }
    img { width: 100%; height: 100%; object-fit: cover; }
    .card-copy { display: grid; gap: 4px; }
    .card-copy span { color: #64748b; font-size: .75rem; font-weight: 700; }
    .card-copy a { color: #0f172a; font-weight: 800; text-decoration: none; }
    .card-copy strong { color: #047857; }
    .card-actions { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
    .state { min-height: 260px; display: grid; place-items: center; gap: 10px; color: #64748b; text-align: center; }
    .state mat-icon { font-size: 44px; width: 44px; height: 44px; color: #94a3b8; }
  `]
})
export class WishlistComponent implements OnInit {
  readonly fallbackImage = 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop';
  items: WishlistItemResponse[] = [];
  pageData: PageResponse<WishlistItemResponse> | null = null;
  busyIds = new Set<number>();
  loading = false;
  page = 0;
  size = 12;

  constructor(
    private wishlistService: WishlistService,
    private cartService: CartService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.wishlistService.list(this.page, this.size).pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: (res) => {
        this.pageData = res.data;
        this.items = res.data?.content || [];
      },
      error: () => this.notification.error('Could not load wishlist')
    });
  }

  changePage(event: PageEvent): void {
    this.page = event.pageIndex;
    this.size = event.pageSize;
    this.load();
  }

  moveToCart(item: WishlistItemResponse): void {
    const variantId = item.product.variants?.[0]?.id;
    if (!variantId) {
      this.notification.error('This product has no available variant');
      return;
    }
    this.busyIds.add(item.id);
    this.cartService.addToCart(variantId, 1).pipe(
      finalize(() => this.busyIds.delete(item.id))
    ).subscribe({
      next: () => this.remove(item, false),
      error: () => this.notification.error('Could not move product to cart')
    });
  }

  remove(item: WishlistItemResponse, notify = true): void {
    this.busyIds.add(item.id);
    this.wishlistService.remove(item.product.id).pipe(
      finalize(() => this.busyIds.delete(item.id))
    ).subscribe({
      next: () => {
        if (notify) this.notification.success('Removed from wishlist');
        this.load();
      },
      error: () => this.notification.error('Could not remove product')
    });
  }

  useFallback(event: Event): void {
    (event.target as HTMLImageElement).src = this.fallbackImage;
  }
}
