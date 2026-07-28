import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Subject, finalize, takeUntil } from 'rxjs';
import { AnalyticsEventType } from '../../../core/models/analytics-event.model';
import { CategoryResponse } from '../../../core/models/category.model';
import { ProductCatalogQuery, StorefrontProduct, StorefrontVariantItem } from '../../../core/models/product.model';
import { RecentlyViewedProductResponse } from '../../../core/models/recently-viewed.model';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { CategoryService } from '../../../core/services/category.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductService } from '../../../core/services/product.service';
import { RecentlyViewedService } from '../../../core/services/recently-viewed.service';
import { WishlistService } from '../../../core/services/wishlist.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, MatButtonModule, MatCheckboxModule, MatIconModule,
    MatPaginatorModule, MatProgressSpinnerModule, MatSelectModule
  ],
  template: `
    <!-- Hero Banner Carousel -->
    <section class="carousel-hero">
      <div class="carousel-viewport" [style.transform]="'translateX(' + (-currentSlide * 100) + '%)'">
        @for (slide of heroSlides; track slide.id) {
          <div class="carousel-slide" [style.background]="slide.bgGradient">
            <div class="slide-content">
              <span class="slide-badge">{{ slide.badge }}</span>
              <h2>{{ slide.title }}</h2>
              <p>{{ slide.subtitle }}</p>
              <div class="slide-actions">
                <button mat-flat-button class="slide-cta" (click)="scrollToCatalog(slide.categoryFilter)">
                  {{ slide.ctaText }}
                  <mat-icon>arrow_forward</mat-icon>
                </button>
              </div>
            </div>
            <div class="slide-image-box">
              <img [src]="slide.imageUrl" [alt]="slide.title">
            </div>
          </div>
        }
      </div>

      <!-- Arrow Controls -->
      <button mat-icon-button type="button" class="carousel-arrow prev" (click)="prevSlide()" aria-label="Slide trước">
        <mat-icon>chevron_left</mat-icon>
      </button>
      <button mat-icon-button type="button" class="carousel-arrow next" (click)="nextSlide()" aria-label="Slide sau">
        <mat-icon>chevron_right</mat-icon>
      </button>

      <!-- Dots Navigation -->
      <div class="carousel-dots">
        @for (slide of heroSlides; track slide.id; let idx = $index) {
          <button
            type="button"
            class="dot"
            [class.active]="currentSlide === idx"
            (click)="goToSlide(idx)"
            [attr.aria-label]="'Slide ' + (idx + 1)">
          </button>
        }
      </div>
    </section>

    <!-- Recently Viewed Section -->
    @if (!loading && recentlyViewed.length > 0) {
      <section class="recently-viewed-section">
        <div class="section-heading">
          <div>
            <span class="eyebrow">Lịch sử xem</span>
            <h2>Sản phẩm vừa xem gần đây</h2>
          </div>
          <button
            mat-stroked-button
            type="button"
            class="clear-history-btn"
            [disabled]="clearingRecentlyViewed"
            (click)="clearRecentlyViewed()">
            <mat-icon>delete_sweep</mat-icon>
            Xóa lịch sử
          </button>
        </div>

        <div class="recently-viewed-list">
          @for (item of recentlyViewed; track item.id) {
            <a class="recently-viewed-item" [routerLink]="['/products', item.product.id]">
              <img
                [src]="item.product.imageUrl || fallbackImage"
                (error)="useFallback($event)"
                [alt]="item.product.name"
              />
              <span>{{ item.product.name }}</span>
              <strong>{{ item.product.price | currency:'USD':'symbol':'1.0-0' }}</strong>
            </a>
          }
        </div>
      </section>
    }

    <section class="catalog-shell" id="catalog-section">
      <aside class="filters" [class.open]="filtersOpen">
        <div class="filter-heading">
          <div><span class="overline">Bộ lọc</span><h2>Tìm đúng sản phẩm</h2></div>
          <button class="icon-button mobile-only" (click)="filtersOpen = false" aria-label="Đóng bộ lọc">
            <mat-icon>close</mat-icon>
          </button>
        </div>

        <label class="field-label" for="catalog-search">Tìm kiếm</label>
        <div class="search-box">
          <mat-icon>search</mat-icon>
          <input id="catalog-search" [(ngModel)]="draftSearch" (keyup.enter)="applyFilters()"
                 placeholder="Tên hoặc mô tả sản phẩm">
          @if (draftSearch) {
            <button class="icon-button" (click)="draftSearch = ''; applyFilters()" aria-label="Xóa tìm kiếm">
              <mat-icon>close</mat-icon>
            </button>
          }
        </div>

        <label class="field-label" for="category">Danh mục</label>
        <select id="category" [(ngModel)]="categoryId">
          <option [ngValue]="undefined">Tất cả danh mục</option>
          @for (category of categories; track category.id) {
            <option [ngValue]="category.id">{{ category.name }}</option>
          }
        </select>

        <span class="field-label">Khoảng giá</span>
        <div class="price-row">
          <input type="number" min="0" [(ngModel)]="minPrice" placeholder="Từ">
          <span>—</span>
          <input type="number" min="0" [(ngModel)]="maxPrice" placeholder="Đến">
        </div>

        <mat-checkbox [(ngModel)]="inStock">Chỉ hiển thị hàng còn trong kho</mat-checkbox>
        @if (priceError) { <p class="validation">{{ priceError }}</p> }
        <button mat-flat-button class="apply-button" (click)="applyFilters()">Áp dụng bộ lọc</button>
        <button mat-button class="reset-button" (click)="resetFilters()">Đặt lại</button>
      </aside>

      @if (filtersOpen) { <button class="backdrop mobile-only" (click)="filtersOpen = false" aria-label="Đóng"></button> }

      <main class="results" aria-live="polite">
        <div class="result-toolbar">
          <div>
            <button mat-stroked-button class="mobile-only filter-trigger" (click)="filtersOpen = true">
              <mat-icon>tune</mat-icon>Bộ lọc
            </button>
            <p><strong>{{ totalElements }}</strong> kết quả
              @if (appliedSearch) { cho “{{ appliedSearch }}” }
            </p>
          </div>
          <label class="sort-control">Sắp xếp
            <select [(ngModel)]="sortValue" (change)="changeSort()">
              <option value="createdAt,DESC">Mới nhất</option>
              <option value="price,ASC">Giá: thấp đến cao</option>
              <option value="price,DESC">Giá: cao đến thấp</option>
              <option value="name,ASC">Tên A–Z</option>
              <option value="stock,DESC">Còn nhiều hàng</option>
            </select>
          </label>
        </div>

        @if (loading) {
          <div class="state"><mat-spinner diameter="42"></mat-spinner><p>Đang chuẩn bị bộ sưu tập…</p></div>
        } @else if (errorMessage) {
          <div class="state error-state"><mat-icon>cloud_off</mat-icon><h2>Chưa thể tải sản phẩm</h2>
            <p>{{ errorMessage }}</p><button mat-flat-button (click)="loadProducts()">Thử lại</button></div>
        } @else if (!products.length) {
          <div class="state empty-state"><mat-icon>search_off</mat-icon><h2>Không tìm thấy sản phẩm phù hợp</h2>
            <p>Thử nới khoảng giá hoặc chọn một danh mục khác.</p>
            <button mat-flat-button (click)="resetFilters()">Xóa bộ lọc</button></div>
        } @else {
          <div class="product-grid">
            @for (product of products; track product.id) {
              <article class="product-card">
                <div class="image-box">
                  <a class="image-link" [routerLink]="['/products', product.id]">
                    <img [src]="(getSelectedVariant(product.id)?.imageUrl || product.imageUrl) || fallbackImage" (error)="useFallback($event)" [alt]="product.name">
                    <span class="category-badge">{{ product.categoryName }}</span>
                    @if (product.availableStock > 0 && product.availableStock < 10) {
                      <span class="scarcity-badge">Chỉ còn {{ product.availableStock }}</span>
                    }
                  </a>
                  <button
                    mat-icon-button
                    type="button"
                    class="wishlist-btn"
                    [class.active]="isWishlisted(product.id)"
                    [disabled]="isWishlistBusy(product.id)"
                    (click)="toggleWishlist(product, $event)"
                    [attr.aria-label]="isWishlisted(product.id) ? 'Remove from wishlist' : 'Add to wishlist'">
                    <mat-icon>{{ isWishlisted(product.id) ? 'favorite' : 'favorite_border' }}</mat-icon>
                  </button>
                </div>

                <div class="card-body">
                  <div class="meta">
                    <span class="meta-label">
                      <mat-icon class="meta-icon">tune</mat-icon>
                      {{ product.variants?.length || 1 }} mẫu mã
                    </span>
                    <span [class.out]="product.availableStock === 0">
                      {{ product.availableStock > 0 ? 'Còn hàng' : 'Tạm hết hàng' }}
                    </span>
                  </div>
                  
                  <a class="product-name" [routerLink]="['/products', product.id]" [title]="product.name">{{ product.name }}</a>
                  
                  <!-- Interactive Variant Selection Buttons like Detail Page -->
                  <div class="card-variant-section">
                    @if (product.variants && product.variants.length > 0) {
                      <div class="variant-chips-list">
                        @for (v of product.variants; track v.id) {
                          <button
                            type="button"
                            class="card-variant-chip"
                            [class.active]="getSelectedVariant(product.id)?.id === v.id"
                            (click)="selectCardVariant(product, v, $event)">
                            {{ v.variantName }}
                          </button>
                        }
                      </div>
                    } @else {
                      <div class="variant-chips-list">
                        <button type="button" class="card-variant-chip active">Bản tiêu chuẩn</button>
                      </div>
                    }
                  </div>

                  <div class="card-footer">
                    <div class="price">
                      <small>Giá chọn</small>
                      <strong>{{ (getSelectedVariant(product.id)?.price || product.minPrice) | currency:'USD':'symbol':'1.0-0' }}</strong>
                    </div>
                    <button mat-flat-button (click)="addToCart(product)" [disabled]="product.availableStock === 0 || addingId === product.id">
                      <mat-icon>{{ addingId === product.id ? 'hourglass_top' : 'add_shopping_cart' }}</mat-icon>
                      {{ addingId === product.id ? 'Đang thêm' : 'Thêm' }}
                    </button>
                  </div>
                </div>
              </article>
            }
          </div>
          <mat-paginator [length]="totalElements" [pageIndex]="query.page" [pageSize]="query.size"
                         [pageSizeOptions]="[8, 12, 24]" (page)="changePage($event)"
                         showFirstLastButtons aria-label="Phân trang sản phẩm"></mat-paginator>
        }
      </main>
    </section>
  `,
  styles: [`
    :host { display:block; color:#17202a; } * { box-sizing:border-box; }
    
    .carousel-hero {
      position: relative; border-radius: 24px; overflow: hidden; margin-bottom: 32px;
      box-shadow: 0 20px 48px -12px rgba(15, 23, 42, 0.35); background: #0f172a;
    }
    .carousel-viewport { display: flex; transition: transform 0.65s cubic-bezier(0.25, 1, 0.5, 1); width: 100%; }
    .carousel-slide {
      min-width: 100%; box-sizing: border-box; padding: 54px 64px;
      display: grid; grid-template-columns: 1.15fr 0.85fr; align-items: center; gap: 48px;
      color: #ffffff; min-height: 440px;
    }
    .slide-content { display: flex; flex-direction: column; gap: 14px; max-width: 720px; z-index: 2; }
    .slide-badge {
      display: inline-block; width: fit-content;
      background: rgba(255, 255, 255, 0.2); backdrop-filter: blur(10px);
      border: 1px solid rgba(255, 255, 255, 0.35); color: #fff;
      padding: 6px 16px; border-radius: 20px; font-size: 0.78rem; font-weight: 800;
      letter-spacing: 0.1em; text-transform: uppercase; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    }
    .slide-content h2 {
      font-size: clamp(2.2rem, 3.8vw, 3.2rem); line-height: 1.08; font-weight: 850;
      margin: 4px 0 0; letter-spacing: -0.035em; color: #ffffff; text-shadow: 0 2px 14px rgba(0,0,0,0.3);
    }
    .slide-content p { color: rgba(241, 245, 249, 0.95); font-size: 1.06rem; line-height: 1.55; margin: 0; max-width: 620px; }
    .slide-actions { margin-top: 14px; display: flex; gap: 14px; }
    .slide-cta {
      background: #fbbf24 !important; color: #0f172a !important; font-weight: 850 !important;
      font-size: 1rem !important; border-radius: 12px !important; padding: 0 28px !important; height: 52px !important;
      box-shadow: 0 6px 20px rgba(251, 191, 36, 0.45); transition: all 0.25s ease !important;
    }
    .slide-cta:hover { transform: translateY(-3px) scale(1.02); background: #f59e0b !important; box-shadow: 0 10px 25px rgba(245, 158, 11, 0.55); }
    .slide-cta mat-icon { margin-left: 8px; font-size: 20px; width: 20px; height: 20px; }
    .slide-image-box {
      position: relative; width: 100%; height: 330px; border-radius: 20px; overflow: hidden;
      box-shadow: 0 20px 40px -10px rgba(0,0,0,0.45); transform: perspective(1000px) rotateY(-3deg);
      transition: transform 0.4s ease, box-shadow 0.4s ease; border: 1px solid rgba(255,255,255,0.15);
    }
    .carousel-slide:hover .slide-image-box { transform: perspective(1000px) rotateY(0deg) scale(1.02); box-shadow: 0 25px 50px -12px rgba(0,0,0,0.55); }
    .slide-image-box img { width: 100%; height: 100%; object-fit: cover; }
    
    .carousel-arrow {
      position: absolute; top: 50%; transform: translateY(-50%); z-index: 4;
      background: rgba(15, 23, 42, 0.6) !important; color: #ffffff !important;
      backdrop-filter: blur(8px); border: 1px solid rgba(255,255,255,0.25) !important;
      width: 48px !important; height: 48px !important;
      display: inline-flex !important; align-items: center !important; justify-content: center !important;
      transition: all 0.2s ease !important;
    }
    .carousel-arrow:hover { background: rgba(15, 23, 42, 0.9) !important; transform: translateY(-50%) scale(1.1); }
    .carousel-arrow.prev { left: 24px; }
    .carousel-arrow.next { right: 24px; }
    .carousel-arrow mat-icon { font-size: 28px; width: 28px; height: 28px; line-height: 28px; margin: 0; }
    
    .carousel-dots {
      position: absolute; bottom: 18px; left: 50%; transform: translateX(-50%); z-index: 4;
      display: flex; gap: 9px; align-items: center; background: rgba(15, 23, 42, 0.45);
      padding: 7px 16px; border-radius: 20px; backdrop-filter: blur(8px); border: 1px solid rgba(255,255,255,0.15);
    }
    .carousel-dots .dot {
      width: 10px; height: 10px; border-radius: 50%; border: 0;
      background: rgba(255, 255, 255, 0.45); cursor: pointer; transition: all 0.25s ease; padding: 0;
    }
    .carousel-dots .dot.active { width: 32px; border-radius: 12px; background: #fbbf24; }

    .eyebrow,.overline { color:#d9a441; font-size:.72rem; font-weight:800; letter-spacing:.14em; text-transform:uppercase; }
    
    .recently-viewed-section {
      display: flex; flex-direction: column; gap: 14px; padding: 18px; margin-bottom: 24px;
      background: #fff; border: 1px solid #e2e8f0; border-radius: 16px;
    }
    .section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
    .section-heading h2 { margin: 3px 0 0; color: #17202a; font-size: 1.2rem; font-weight: 800; }
    .clear-history-btn { border-radius: 8px; font-size: 0.82rem; }
    .recently-viewed-list {
      display: grid; grid-auto-flow: column; grid-auto-columns: minmax(180px, 220px);
      gap: 12px; overflow-x: auto; padding: 4px 0 10px;
    }
    .recently-viewed-item {
      display: grid; grid-template-columns: 52px 1fr; grid-template-rows: auto auto;
      gap: 2px 10px; align-items: center; min-height: 68px; padding: 8px;
      border: 1px solid #e4e7e5; border-radius: 10px; background: #fafbfc; color: inherit; text-decoration: none;
      transition: all 0.2s ease;
    }
    .recently-viewed-item:hover { border-color: #d9a441; background: #fff; box-shadow: 0 4px 12px rgba(16,42,42,0.08); }
    .recently-viewed-item img { grid-row: 1 / span 2; width: 52px; height: 52px; border-radius: 8px; object-fit: cover; background: #eef1ef; }
    .recently-viewed-item span { overflow: hidden; color: #17202a; font-size: 0.84rem; font-weight: 750; line-height: 1.25; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
    .recently-viewed-item strong { color: #173d38; font-size: 0.84rem; font-weight: 800; }

    .catalog-shell { display:grid; grid-template-columns:260px minmax(0,1fr); gap:24px; align-items:start; }
    .filters { position:sticky; top:94px; background:#fff; border:1px solid #e2e8f0; padding:22px; border-radius:16px; }
    .filter-heading { display:flex; justify-content:space-between; }.filter-heading h2 { font-size:1.15rem; margin:4px 0 22px; }
    .field-label { display:block; font-size:.76rem; font-weight:800; margin:18px 0 7px; color:#53615e; }
    select,input { border:1px solid #d9dfdc; border-radius:9px; background:#fff; color:#17202a; min-height:42px; padding:0 11px; width:100%; font:inherit; }
    select:focus,input:focus { outline:2px solid #b78a34; outline-offset:1px; }
    .search-box { display:flex; align-items:center; border:1px solid #d9dfdc; border-radius:9px; padding:0 8px; }
    .search-box input { border:0; outline:0; padding:0 7px; min-width:0; }.search-box mat-icon { color:#7a8784; }
    .icon-button { border:0; background:transparent; width:auto; padding:4px; display:flex; cursor:pointer; }
    .price-row { display:grid; grid-template-columns:1fr auto 1fr; align-items:center; gap:7px; }
    mat-checkbox { display:block; margin:17px 0; font-size:.86rem; }.apply-button { width:100%; background:#b18432!important; color:#fff!important; }
    .reset-button { width:100%; margin-top:4px; }.validation { color:#b42318; font-size:.78rem; }
    .result-toolbar { min-height:48px; display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
    .result-toolbar p { margin:0; color:#687572; }.sort-control { display:flex; align-items:center; gap:10px; color:#687572; font-size:.82rem; }
    .sort-control select { width:190px; }
    .product-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:18px; }
    @media(max-width:1380px) { .product-grid { grid-template-columns:repeat(3,minmax(0,1fr)); } }
    @media(max-width:960px) { .product-grid { grid-template-columns:repeat(2,minmax(0,1fr)); } }
    @media(max-width:560px) { .product-grid { grid-template-columns:1fr; } }
    @media(max-width:880px) { .carousel-slide { grid-template-columns:1fr; padding:32px 24px; }.slide-image-box { display:none; } }
    .product-card { background:#fff; border:1px solid #e2e8f0; border-radius:16px; overflow:hidden; transition:all 0.25s cubic-bezier(0.4,0,0.2,1); min-width:0; display:flex; flex-direction:column; position:relative; }
    .product-card:hover { transform:translateY(-4px); border-color:#cbd5e1; box-shadow:0 12px 28px -6px rgba(15,23,42,0.12); }
    .image-box { position:relative; width:100%; aspect-ratio: 1 / 1; overflow: hidden; background: #f8fafc; }
    .image-link { position:relative; display:block; background:#f8fafc; width:100%; height:100%; overflow:hidden; }
    .image-link img { width:100%; height:100%; object-fit:cover; object-position:center; transition:transform 0.4s ease; }.product-card:hover img { transform:scale(1.04); }
    .category-badge, .scarcity-badge {
      position: absolute; top: 12px; left: 12px; z-index: 2;
      background: rgba(255, 255, 255, 0.92); backdrop-filter: blur(8px);
      color: #334155; padding: 4px 9px; border-radius: 8px;
      font-size: 0.72rem; font-weight: 700; border: 1px solid rgba(226, 232, 240, 0.8);
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05); pointer-events: none;
    }
    .scarcity-badge {
      top: 44px; left: 12px;
      background: rgba(254, 243, 199, 0.94); color: #92400e; border-color: #fde68a;
    }
    .wishlist-btn {
      position: absolute !important; top: 12px !important; right: 12px !important; z-index: 3;
      width: 38px !important; height: 38px !important; min-width: 38px !important; padding: 0 !important;
      display: inline-flex !important; align-items: center !important; justify-content: center !important;
      background: rgba(255, 255, 255, 0.92) !important; backdrop-filter: blur(8px);
      color: #64748b !important; border: 1px solid rgba(226, 232, 240, 0.9) !important;
      border-radius: 50% !important;
      box-shadow: 0 4px 12px rgba(15, 23, 42, 0.12);
      transition: all 0.2s ease-in-out !important;
    }
    .wishlist-btn mat-icon {
      font-size: 20px !important; width: 20px !important; height: 20px !important; line-height: 20px !important;
      display: flex !important; align-items: center !important; justify-content: center !important;
      margin: 0 !important; padding: 0 !important;
    }
    .wishlist-btn:hover {
      transform: scale(1.08) !important; background: #ffffff !important; color: #e11d48 !important;
      box-shadow: 0 6px 16px rgba(225, 29, 72, 0.2) !important;
    }
    .wishlist-btn.active {
      color: #e11d48 !important; background: #fff1f2 !important; border-color: #fecdd3 !important;
    }
    .card-body { padding:16px; display:flex; flex-direction:column; flex:1; justify-space-between: space-between; }
    .meta { display:flex; justify-content:space-between; align-items:center; color:#475569; font-size:.73rem; font-weight:700; height:22px; }
    .meta-label { display:inline-flex; align-items:center; gap:4px; color:#475569; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:80%; }
    .meta-icon { font-size:14px; width:14px; height:14px; line-height:14px; color:#64748b; flex-shrink:0; }
    .meta .out { color:#e11d48; font-weight:750; flex-shrink:0; }
    .product-name {
      color:#0f172a; font-size:1.02rem; line-height:1.35; font-weight:800; text-decoration:none;
      display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical;
      overflow:hidden; text-overflow:ellipsis;
      height:2.7em; margin:8px 0 10px;
    }
    .card-variant-section {
      margin: 4px 0 14px; height: 38px; overflow: hidden; display: flex; align-items: center;
    }
    .variant-chips-list {
      display: flex; flex-wrap: wrap; gap: 6px; width: 100%; align-content: flex-start;
    }
    .card-variant-chip {
      background: #f8fafc; color: #475569; font-size: 0.73rem; font-weight: 650;
      padding: 4px 10px; border-radius: 8px; border: 1px solid #cbd5e1; cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); outline: none; line-height: 1.3;
      display: inline-flex; align-items: center; white-space: nowrap; max-width: 100%; overflow: hidden; text-overflow: ellipsis;
    }
    .card-variant-chip:hover {
      background: #f1f5f9; border-color: #94a3b8; color: #0f172a;
    }
    .card-variant-chip.active {
      background: #eff6ff !important; border-color: #2563eb !important; color: #1d4ed8 !important;
      font-weight: 750 !important; box-shadow: 0 2px 6px rgba(37, 99, 235, 0.18);
    }
    .card-footer { border-top:1px solid #edf0ee; padding-top:12px; margin-top:auto; display:flex; align-items:flex-end; justify-content:space-between; gap:8px; height:52px; }
    .price { display:flex; flex-wrap:wrap; align-items:baseline; gap:4px; }.price small { width:100%; color:#7a8784; }.price strong { font-size:1.25rem; color:#173d38; }
    .price span { font-size:.72rem; color:#7a8784; }.card-footer button { background:#173d38!important; color:#fff!important; min-width:86px; border-radius:9px!important; }
    .state { min-height:420px; border:1px dashed #ccd4d0; border-radius:16px; display:flex; flex-direction:column; align-items:center; justify-content:center; text-align:center; color:#687572; }
    .state>mat-icon { font-size:48px; width:48px; height:48px; color:#9aa6a2; }.state h2 { color:#26332f; margin:12px 0 0; }
    mat-paginator { margin-top:22px; border:1px solid #e4e7e5; border-radius:12px; }.mobile-only { display:none; }.backdrop { display:none; }
    @media(max-width:1050px) { .product-grid { grid-template-columns:repeat(2,minmax(0,1fr)); } }
    @media(max-width:760px) {
      .hero { padding:28px 23px; border-radius:18px; }.hero-stat { display:none; }.catalog-shell { grid-template-columns:1fr; }
      .mobile-only { display:inline-flex; }.filters { display:none; position:fixed; z-index:1002; top:0; left:0; bottom:0; width:min(88vw,340px); border-radius:0; overflow:auto; }
      .filters.open { display:block; }.backdrop { display:block; position:fixed; z-index:1001; inset:0; width:100%; border:0; background:rgba(7,23,21,.48); }
      .result-toolbar { align-items:flex-end; }.result-toolbar>div { display:flex; flex-direction:column; gap:8px; }.filter-trigger { width:max-content; }
      .sort-control { align-items:flex-start; flex-direction:column; gap:3px; }.sort-control select { width:165px; }
    }
    @media(max-width:520px) { .product-grid { grid-template-columns:1fr; }.hero h1 { font-size:2.1rem; } }
  `]
})
export class ProductListComponent implements OnInit, OnDestroy {
  readonly fallbackImage = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800';
  private readonly destroy$ = new Subject<void>();
  
  heroSlides = [
    {
      id: 1,
      badge: 'Bộ sưu tập Flagship 2026',
      title: 'Công nghệ tiên phong. Trải nghiệm đỉnh cao.',
      subtitle: 'Sở hữu ngay các dòng máy tính OLED, tablet M4 & tai nghe chống ồn mới nhất với ưu đãi độc quyền.',
      ctaText: 'Khám phá ngay',
      categoryFilter: undefined,
      bgGradient: 'linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%)',
      imageUrl: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=1200&q=85'
    },
    {
      id: 2,
      badge: 'Gaming & Đồ họa',
      title: 'Chiến game bứt phá. Hiệu năng tối đa.',
      subtitle: 'Cấu hình khủng từ laptop gaming ROG Zephyrus và Nintendo Switch OLED cho game thủ chuyên nghiệp.',
      ctaText: 'Xem sản phẩm Gaming',
      categoryFilter: 5,
      bgGradient: 'linear-gradient(135deg, #1a0933 0%, #3b1566 50%, #5c238b 100%)',
      imageUrl: 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?auto=format&fit=crop&w=1200&q=85'
    },
    {
      id: 3,
      badge: 'Âm thanh & Phụ kiện cao cấp',
      title: 'Âm thanh chân thực. Không gian sống động.',
      subtitle: 'Trải nghiệm tai nghe Bose QuietComfort Ultra & loa Marshall Stanmore III chính hãng.',
      ctaText: 'Khám phá Âm thanh',
      categoryFilter: 4,
      bgGradient: 'linear-gradient(135deg, #1b2a1a 0%, #2e4a2d 50%, #446e42 100%)',
      imageUrl: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1200&q=85'
    }
  ];

  currentSlide = 0;
  private carouselInterval: any;

  products: StorefrontProduct[] = [];
  selectedVariantMap: { [productId: number]: StorefrontVariantItem } = {};
  categories: CategoryResponse[] = [];
  recentlyViewed: RecentlyViewedProductResponse[] = [];
  wishlistIds = new Set<number>();
  wishlistBusyIds = new Set<number>();
  totalElements = 0;
  loading = false;
  clearingRecentlyViewed = false;
  errorMessage = '';
  addingId?: number;
  filtersOpen = false;
  draftSearch = '';
  appliedSearch = '';
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  inStock = false;
  priceError = '';
  sortValue = 'createdAt,DESC';
  query: ProductCatalogQuery = { page: 0, size: 12, sortBy: 'createdAt', sortDir: 'DESC' };

  constructor(
    private productService: ProductService,
    private categoryService: CategoryService,
    private cartService: CartService,
    private wishlistService: WishlistService,
    private recentlyViewedService: RecentlyViewedService,
    private analyticsService: AnalyticsService,
    private authService: AuthService,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.draftSearch = params.get('q') || '';
    this.appliedSearch = this.draftSearch;
    this.categoryId = this.numberParam(params.get('category'));
    this.minPrice = this.numberParam(params.get('minPrice'));
    this.maxPrice = this.numberParam(params.get('maxPrice'));
    this.inStock = params.get('inStock') === 'true';
    this.query.page = Math.max(0, Number(params.get('page')) || 0);
    this.sortValue = params.get('sort') || this.sortValue;
    this.setSort();

    this.categoryService.getAll().pipe(takeUntil(this.destroy$)).subscribe({
      next: response => this.categories = response.data || [],
      error: () => this.notification.error('Không thể tải danh mục')
    });

    this.loadProducts();
    this.loadRecentlyViewed();
    this.startCarouselAutoPlay();
  }

  ngOnDestroy(): void {
    this.stopCarouselAutoPlay();
    this.destroy$.next();
    this.destroy$.complete();
  }

  startCarouselAutoPlay(): void {
    this.carouselInterval = setInterval(() => {
      this.nextSlide();
    }, 5000);
  }

  stopCarouselAutoPlay(): void {
    if (this.carouselInterval) {
      clearInterval(this.carouselInterval);
    }
  }

  getVariantChips(variantNamesStr?: string): string[] {
    if (!variantNamesStr) return ['Chính hãng'];
    const items = variantNamesStr
      .split(',')
      .map(s => {
        let t = s.trim();
        if (/^Standard Variant\s*0?(\d+)$/i.test(t)) {
          return `Tùy chọn ${t.replace(/^Standard Variant\s*0?/i, '')}`;
        }
        if (/^DEMO-SKU-/i.test(t)) {
          return 'Chính hãng';
        }
        return t;
      })
      .filter(Boolean);
    return items.length > 0 ? items.slice(0, 3) : ['Chính hãng'];
  }

  getVariantSummaryText(product: StorefrontProduct): string {
    const chips = this.getVariantChips(product.variantNames);
    if (chips.length > 0) {
      return `Phân loại: ${chips.join(', ')}`;
    }
    return 'Hàng chính hãng';
  }

  nextSlide(): void {
    this.currentSlide = (this.currentSlide + 1) % this.heroSlides.length;
  }

  prevSlide(): void {
    this.currentSlide = (this.currentSlide - 1 + this.heroSlides.length) % this.heroSlides.length;
    this.resetCarouselTimer();
  }

  goToSlide(index: number): void {
    this.currentSlide = index;
    this.resetCarouselTimer();
  }

  resetCarouselTimer(): void {
    this.stopCarouselAutoPlay();
    this.startCarouselAutoPlay();
  }

  scrollToCatalog(categoryFilter?: number): void {
    if (categoryFilter !== undefined) {
      this.categoryId = categoryFilter;
      this.applyFilters();
    }
    const catalogElement = document.getElementById('catalog-section');
    if (catalogElement) {
      catalogElement.scrollIntoView({ behavior: 'smooth' });
    }
  }

  selectCardVariant(product: StorefrontProduct, variant: StorefrontVariantItem, event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();
    this.selectedVariantMap[product.id] = variant;
  }

  getSelectedVariant(productId: number): StorefrontVariantItem | undefined {
    return this.selectedVariantMap[productId];
  }

  loadProducts(): void {
    this.loading = true;
    this.errorMessage = '';
    this.query = { ...this.query, q: this.appliedSearch || undefined, categoryId: this.categoryId,
      minPrice: this.minPrice, maxPrice: this.maxPrice, inStock: this.inStock };
    
    this.productService.browseProducts(this.query).pipe(
      takeUntil(this.destroy$), finalize(() => this.loading = false)
    ).subscribe({
      next: response => {
        this.products = response.data.content;
        this.totalElements = response.data.totalElements;
        this.products.forEach(p => {
          if (p.variants && p.variants.length > 0) {
            this.selectedVariantMap[p.id] = p.variants[0];
          }
        });
        this.loadWishlistStatuses();
      },
      error: () => this.errorMessage = 'Kết nối đến cửa hàng bị gián đoạn. Vui lòng thử lại.'
    });
  }

  applyFilters(): void {
    if (this.minPrice !== undefined && this.maxPrice !== undefined && this.minPrice > this.maxPrice) {
      this.priceError = 'Giá tối thiểu không thể lớn hơn giá tối đa.';
      return;
    }
    this.priceError = '';
    this.appliedSearch = this.draftSearch.trim();
    if (this.appliedSearch) {
      this.analyticsService.track(AnalyticsEventType.Search, { query: this.appliedSearch });
    }
    this.query.page = 0;
    this.filtersOpen = false;
    this.syncUrl();
    this.loadProducts();
  }

  resetFilters(): void {
    this.draftSearch = ''; this.appliedSearch = ''; this.categoryId = undefined;
    this.minPrice = undefined; this.maxPrice = undefined; this.inStock = false; this.priceError = '';
    this.query.page = 0; this.filtersOpen = false; this.syncUrl(); this.loadProducts();
  }

  changeSort(): void { this.setSort(); this.query.page = 0; this.syncUrl(); this.loadProducts(); }

  changePage(event: PageEvent): void {
    this.query.page = event.pageIndex; this.query.size = event.pageSize; this.syncUrl(); this.loadProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  addToCart(product: StorefrontProduct): void {
    if (!this.authService.isAuthenticated()) {
      this.notification.error('Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng');
      this.router.navigate(['/login']);
      return;
    }
    const selectedVariant = this.getSelectedVariant(product.id);
    const targetVariantId = selectedVariant?.id;
    if (!targetVariantId) {
      this.notification.error('Sản phẩm chưa có lựa chọn khả dụng');
      return;
    }
    this.addingId = product.id;
    this.cartService.addToCart(targetVariantId, 1).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.addingId = undefined)
    ).subscribe({
      next: (res) => {
        if (res.success) {
          this.analyticsService.track(AnalyticsEventType.AddToCart, {
            productId: product.id,
            productName: product.name,
            variantId: targetVariantId,
            quantity: 1
          });
          this.notification.success(`Đã thêm “${product.name}” (${selectedVariant?.variantName || 'Mặc định'}) vào giỏ`);
        } else {
          this.notification.error(res.message || 'Không thể thêm vào giỏ hàng');
        }
      },
      error: error => this.notification.error(error.error?.message || 'Không thể thêm vào giỏ hàng')
    });
  }

  toggleWishlist(product: StorefrontProduct, event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    if (this.isWishlistBusy(product.id)) return;

    const wasWishlisted = this.isWishlisted(product.id);
    this.setWishlistBusy(product.id, true);
    this.setWishlisted(product.id, !wasWishlisted);

    const observer = {
      next: () => {
        this.setWishlistBusy(product.id, false);
        if (!wasWishlisted) {
          this.analyticsService.track(AnalyticsEventType.AddToWishlist, {
            productId: product.id,
            productName: product.name
          });
        }
        this.notification.success(wasWishlisted ? 'Đã xóa khỏi yêu thích' : 'Đã thêm vào yêu thích');
      },
      error: (err: HttpErrorResponse) => {
        this.setWishlisted(product.id, wasWishlisted);
        this.setWishlistBusy(product.id, false);
        this.notification.error(err.error?.message || 'Không thể cập nhật danh sách yêu thích');
      }
    };

    if (wasWishlisted) {
      this.wishlistService.remove(product.id).subscribe(observer);
    } else {
      this.wishlistService.add(product.id).subscribe(observer);
    }
  }

  isWishlisted(productId: number): boolean {
    return this.wishlistIds.has(productId);
  }

  isWishlistBusy(productId: number): boolean {
    return this.wishlistBusyIds.has(productId);
  }

  clearRecentlyViewed(): void {
    if (this.clearingRecentlyViewed) return;
    this.clearingRecentlyViewed = true;
    this.recentlyViewedService.clear().subscribe({
      next: () => {
        this.recentlyViewed = [];
        this.clearingRecentlyViewed = false;
        this.notification.success('Đã xóa lịch sử xem gần đây');
      },
      error: (err: HttpErrorResponse) => {
        this.clearingRecentlyViewed = false;
        this.notification.error(err.error?.message || 'Không thể xóa lịch sử xem');
      }
    });
  }

  useFallback(event: Event): void { (event.target as HTMLImageElement).src = this.fallbackImage; }

  private setSort(): void {
    const [sortBy, sortDir] = this.sortValue.split(',');
    this.query.sortBy = sortBy as ProductCatalogQuery['sortBy'];
    this.query.sortDir = sortDir as ProductCatalogQuery['sortDir'];
  }

  private syncUrl(): void {
    this.router.navigate([], { relativeTo: this.route, replaceUrl: true, queryParams: {
      q: this.appliedSearch || null, category: this.categoryId ?? null, minPrice: this.minPrice ?? null,
      maxPrice: this.maxPrice ?? null, inStock: this.inStock || null, sort: this.sortValue,
      page: this.query.page || null
    }});
  }

  private numberParam(value: string | null): number | undefined {
    if (value === null || value === '') return undefined;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  private loadWishlistStatuses(): void {
    this.products.forEach(product => {
      this.wishlistService.status(product.id).subscribe({
        next: (res) => this.setWishlisted(product.id, !!res.data?.wishlisted),
        error: () => this.setWishlisted(product.id, false)
      });
    });
  }

  private setWishlisted(productId: number, wishlisted: boolean): void {
    wishlisted ? this.wishlistIds.add(productId) : this.wishlistIds.delete(productId);
  }

  private setWishlistBusy(productId: number, busy: boolean): void {
    busy ? this.wishlistBusyIds.add(productId) : this.wishlistBusyIds.delete(productId);
  }

  private loadRecentlyViewed(): void {
    this.recentlyViewedService.list(8).subscribe({
      next: (res) => this.recentlyViewed = res.success && res.data ? res.data : [],
      error: () => this.recentlyViewed = []
    });
  }
}
