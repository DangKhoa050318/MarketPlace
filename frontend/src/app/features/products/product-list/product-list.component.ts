import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProductResponse } from '../../../core/models/product.model';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="product-page-container">
      <!-- Page Header -->
      <div class="product-header">
        <div>
          <span class="eyebrow">Storefront Collection</span>
          <h1 class="page-title">Featured Storefront Catalog</h1>
          <p class="page-subtitle">Discover premium items crafted for seamless order processing</p>
        </div>

        <!-- Search Bar Component -->
        <div class="search-bar-container">
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Search products...</mat-label>
            <input
              matInput
              type="text"
              [(ngModel)]="searchQuery"
              (keyup.enter)="onSearch()"
              placeholder="e.g. Laptop, Mouse..."
            />
            <button *ngIf="searchQuery" matSuffix mat-icon-button aria-label="Clear" (click)="clearSearch()">
              <mat-icon>close</mat-icon>
            </button>
            <button matSuffix mat-icon-button (click)="onSearch()" aria-label="Search">
              <mat-icon class="search-icon">search</mat-icon>
            </button>
          </mat-form-field>
        </div>
      </div>

      @if (loading) {
        <div class="loading-container">
          <mat-spinner diameter="44"></mat-spinner>
        </div>
      }

      <!-- USER VIEW: Solid Clean Product Grid -->
      @if (!loading) {
        <div class="storefront-grid">
          @for (product of products; track product.id) {
            <div class="tilt-card-container">
              <div class="tilt-card surface-card surface-card-hover product-card">
                <div class="product-image-box">
                  <img [src]="product.imageUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500'"
                       (error)="onImageError($event)" [alt]="product.name" class="product-img" />
                  <div class="category-pill-badge">{{ product.categoryName || 'General' }}</div>
                </div>

                <div class="product-info">
                  <h3 class="product-title">{{ product.name }}</h3>
                  <p class="product-slug-text">{{ product.slug }}</p>

                  <div class="price-stock-row">
                    <div class="price-tag">
                      \${{ product.price | number:'1.2-2' }}
                    </div>

                    <div class="stock-indicator" [class.in-stock]="product.stock > 0" [class.out-of-stock]="product.stock <= 0">
                      <mat-icon class="stock-icon">{{ product.stock > 0 ? 'check_circle' : 'cancel' }}</mat-icon>
                      <span>{{ product.stock > 0 ? product.stock + ' in stock' : 'Out of stock' }}</span>
                    </div>
                  </div>

                  <button 
                    mat-raised-button 
                    class="btn-solid-primary add-cart-btn" 
                    (click)="onAddToCart(product)" 
                    [disabled]="!product.active || product.stock <= 0">
                    <mat-icon>shopping_bag</mat-icon> Add to Cart
                  </button>
                </div>
              </div>
            </div>
          } @empty {
            <div class="empty-state surface-card">
              <mat-icon class="empty-icon">search_off</mat-icon>
              <h3>No products found matching "{{ searchQuery }}"</h3>
              <button mat-button color="primary" (click)="clearSearch()" *ngIf="searchQuery">Clear Search Filter</button>
            </div>
          }
        </div>
      }

      <div class="pagination-wrapper surface-card">
        <mat-paginator
          [length]="totalElements"
          [pageSize]="pageSize"
          [pageIndex]="currentPage"
          [pageSizeOptions]="[8, 16, 32]"
          (page)="onPageChange($event)"
          showFirstLastButtons
        ></mat-paginator>
      </div>
    </div>
  `,
  styles: [`
    .product-page-container {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .product-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
    }

    .eyebrow {
      color: #0284c7;
      font-size: .7rem;
      font-weight: 800;
      letter-spacing: .13em;
      text-transform: uppercase;
    }

    .page-title {
      margin: 4px 0 0 0;
      font-size: 2rem;
      font-weight: 800;
      letter-spacing: -0.5px;
      color: var(--text-main);
    }

    .page-subtitle {
      margin: 4px 0 0 0;
      color: var(--text-muted);
      font-size: 0.95rem;
    }

    .search-bar-container {
      min-width: 300px;
    }

    .search-field {
      width: 100%;
      margin-bottom: -1.25em;
    }

    .search-icon {
      color: #4f46e5;
    }

    /* Storefront Grid & Solid Cards */
    .storefront-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;
    }

    .product-card {
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 16px;
      height: 100%;
      box-sizing: border-box;
    }

    .product-image-box {
      position: relative;
      width: 100%;
      height: 200px;
      border-radius: 12px;
      overflow: hidden;
      background: #f1f5f9;
    }

    .product-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
    }

    .product-card:hover .product-img {
      transform: scale(1.05);
    }

    .category-pill-badge {
      position: absolute;
      top: 12px;
      right: 12px;
      background: #0284c7;
      color: #ffffff;
      padding: 4px 10px;
      border-radius: 8px;
      font-size: 0.75rem;
      font-weight: 700;
      box-shadow: 0 2px 6px rgba(2, 132, 199, 0.25);
    }

    .product-info {
      display: flex;
      flex-direction: column;
      gap: 8px;
      flex: 1;
    }

    .product-title {
      margin: 0;
      font-size: 1.15rem;
      font-weight: 700;
      color: var(--text-main);
    }

    .product-slug-text {
      margin: 0;
      font-size: 0.8rem;
      color: var(--text-muted);
    }

    .price-stock-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: auto;
      padding-top: 12px;
    }

    .price-tag {
      font-size: 1.35rem;
      font-weight: 800;
      color: #4f46e5;
    }

    .stock-indicator {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 0.75rem;
      font-weight: 600;
    }

    .stock-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }

    .in-stock { color: #10b981; }
    .out-of-stock { color: #ef4444; }

    .add-cart-btn {
      width: 100%;
      margin-top: 12px;
      height: 42px;
    }

    .pagination-wrapper {
      padding: 8px 16px;
    }

    .loading-container {
      display: flex;
      justify-content: center;
      padding: 40px;
    }

    .empty-state {
      grid-column: 1 / -1;
      padding: 60px;
      text-align: center;
      color: var(--text-muted);
    }

    .empty-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #0284c7;
    }
  `]
})
export class ProductListComponent implements OnInit {
  products: ProductResponse[] = [];
  totalElements = 0;
  pageSize = 8;
  currentPage = 0;
  searchQuery = '';
  loading = false;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    const request$ = (this.searchQuery && this.searchQuery.trim().length > 0)
      ? this.productService.searchProducts(this.searchQuery.trim(), this.currentPage, this.pageSize)
      : this.productService.getProducts(this.currentPage, this.pageSize);

    request$.subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.data) {
          this.products = res.data.content;
          this.totalElements = res.data.totalElements;
        }
      },
      error: () => {
        this.loading = false;
        this.notification.error('Failed to load product catalog');
      }
    });
  }

  onSearch(): void {
    this.currentPage = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    this.loadProducts();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.currentPage = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    this.loadProducts();
  }

  onAddToCart(product: ProductResponse): void {
    this.cartService.addToCart(product.id, 1).subscribe({
      next: (res) => {
        if (res.success) {
          this.notification.success(`Added "${product.name}" to shopping cart!`);
        }
      },
      error: (err) => {
        this.notification.error(err.error?.message || 'Failed to add product to cart');
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }

  onImageError(event: Event): void {
    (event.target as HTMLImageElement).src = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500';
  }
}
