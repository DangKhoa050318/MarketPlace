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
import { Subject, debounceTime, distinctUntilChanged, finalize, switchMap, takeUntil, tap } from 'rxjs';
import {
  AnalyticsEventSource,
  AnalyticsEventType,
  RecommendationPlacement
} from '../../../core/models/analytics-event.model';
import { CategoryResponse } from '../../../core/models/category.model';
import { ProductCatalogQuery, StorefrontProduct, StorefrontVariantItem, SuggestResult } from '../../../core/models/product.model';
import { RecentlyViewedProductResponse } from '../../../core/models/recently-viewed.model';
import { ScrollRevealDirective } from '../../../shared/directives/scroll-reveal.directive';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { CategoryService } from '../../../core/services/category.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductService } from '../../../core/services/product.service';
import { ReviewService } from '../../../core/services/review.service';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { RecentlyViewedService } from '../../../core/services/recently-viewed.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import {
  RecommendationCarouselComponent
} from '../../../shared/components/recommendation-carousel/recommendation-carousel.component';
import {
  StorefrontMerchandisingComponent
} from '../../storefront/merchandising/storefront-merchandising.component';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, MatButtonModule, MatCheckboxModule, MatIconModule,
    MatPaginatorModule, MatProgressSpinnerModule, MatSelectModule, RecommendationCarouselComponent,
    ScrollRevealDirective, StorefrontMerchandisingComponent
  ],
  template: `
    <!-- Hero Banner Carousel -->
    <section class="carousel-hero" appScrollReveal>
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

    <app-storefront-merchandising appScrollReveal></app-storefront-merchandising>

    <app-recommendation-carousel appScrollReveal
      [title]="query.categoryId ? 'Bán chạy trong danh mục' : 'Sản phẩm bán chạy'"
      [placement]="query.categoryId
        ? recommendationPlacement.CategoryBestSellers
        : recommendationPlacement.HomeBestSellers"
      [categoryId]="query.categoryId"
      [limit]="10">
    </app-recommendation-carousel>

    <!-- Recently Viewed Section -->
    @if (!loading && recentlyViewed.length > 0) {
      <section class="recently-viewed-section" appScrollReveal>
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
              <strong>{{ item.product.price | currency:'VND':'symbol':'1.0-0' }}</strong>
            </a>
          }
        </div>
      </section>
    }

    <section class="catalog-shell" appScrollReveal id="catalog-section">
      <aside class="filters" [class.open]="filtersOpen">
        <div class="filter-heading">
          <div><span class="overline">Bộ lọc</span><h2>Tìm đúng sản phẩm</h2></div>
          <button class="icon-button mobile-only" (click)="filtersOpen = false" aria-label="Đóng bộ lọc">
            <mat-icon>close</mat-icon>
          </button>
        </div>

        <label class="field-label" for="catalog-search">Tìm kiếm</label>
        <div class="search-wrap">
          <div class="search-box" [class.focused]="searchFocused">
            <mat-icon class="search-prefix">search</mat-icon>
            <input id="catalog-search" [(ngModel)]="draftSearch"
                   (keyup.enter)="commitInstantSearch()"
                   (focus)="onSearchFocus()"
                   (blur)="onSearchBlur()"
                   (input)="onSearchInput()"
                   autocomplete="off"
                   placeholder="Tìm tên, mô tả hoặc phân loại…">
            @if (draftSearch) {
              <button class="icon-button" (click)="clearSearch()" aria-label="Xóa tìm kiếm">
                <mat-icon>close</mat-icon>
              </button>
            }
          </div>

          <!-- Autocomplete / Recent Searches Dropdown -->
          @if ((suggestions.length > 0 || recentSearches.length > 0) && searchFocused) {
            <div class="search-dropdown" (mousedown)="$event.preventDefault()">
              @if (suggestions.length > 0) {
                <div class="dropdown-section">
                  <span class="dropdown-label">Gợi ý sản phẩm</span>
                  @for (s of suggestions; track s.id) {
                    <button type="button" class="dropdown-item" (click)="selectSuggestion(s.name)"
                            [routerLink]="['/products', s.id]">
                      <img [src]="s.imageUrl || fallbackImage" (error)="useFallback($event)"
                           class="suggest-thumb" alt="">
                      <div class="suggest-info">
                        <span class="suggest-name" [innerHTML]="highlightMatch(s.name, draftSearch)"></span>
                        <span class="suggest-price">{{ s.minPrice | currency:'VND':'symbol':'1.0-0' }}</span>
                      </div>
                    </button>
                  }
                </div>
              }
              @if (suggestions.length === 0 && recentSearches.length > 0) {
                <div class="dropdown-section">
                  <span class="dropdown-label">Tìm gần đây</span>
                  @for (s of recentSearches; track s) {
                    <button type="button" class="dropdown-item" (click)="selectSuggestion(s)">
                      <mat-icon>schedule</mat-icon>
                      <span>{{ s }}</span>
                    </button>
                  }
                  <button type="button" class="dropdown-item clear-recent" (click)="clearRecentSearches()">
                    <mat-icon>delete_sweep</mat-icon>
                    <span>Xóa lịch sử</span>
                  </button>
                </div>
              }
            </div>
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

        <!-- Active Filter Chips -->
        @if (hasActiveFilters) {
          <div class="filter-chips">
            @if (appliedSearch) {
              <span class="chip">
                <mat-icon>search</mat-icon> “{{ appliedSearch }}”
                <button class="chip-remove" (click)="removeFilter('search')"><mat-icon>close</mat-icon></button>
              </span>
            }
            @if (categoryId) {
              <span class="chip">
                <mat-icon>category</mat-icon> {{ getCategoryName(categoryId) }}
                <button class="chip-remove" (click)="removeFilter('category')"><mat-icon>close</mat-icon></button>
              </span>
            }
            @if (minPrice !== undefined || maxPrice !== undefined) {
              <span class="chip">
                <mat-icon>attach_money</mat-icon>
                @if (minPrice !== undefined && maxPrice !== undefined) {
                  \${{ minPrice }} – \${{ maxPrice }}
                } @else if (minPrice !== undefined) {
                  ≥ \${{ minPrice }}
                } @else {
                  ≤ \${{ maxPrice }}
                }
                <button class="chip-remove" (click)="removeFilter('price')"><mat-icon>close</mat-icon></button>
              </span>
            }
            @if (inStock) {
              <span class="chip">
                <mat-icon>inventory_2</mat-icon> Còn hàng
                <button class="chip-remove" (click)="removeFilter('inStock')"><mat-icon>close</mat-icon></button>
              </span>
            }
            <button class="chip-clear-all" (click)="resetFilters()">Xóa tất cả</button>
          </div>
        }

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
                  
                  <a class="product-name" [routerLink]="['/products', product.id]" [title]="product.name"
                     [innerHTML]="highlightMatch(product.name, appliedSearch)"></a>

                  @if (product.reviewCount) {
                    <a class="card-rating" [routerLink]="['/products', product.id]"
                       [attr.aria-label]="product.averageRating + ' stars from ' + product.reviewCount + ' reviews'">
                      <span class="card-stars">
                        @for (s of [1, 2, 3, 4, 5]; track s) {
                          <mat-icon [class.filled]="s <= (product.averageRating || 0)">{{ s <= (product.averageRating || 0) ? 'star' : 'star_border' }}</mat-icon>
                        }
                      </span>
                      <small>{{ product.averageRating }} ({{ product.reviewCount }})</small>
                    </a>
                  }

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
                      <strong>{{ (getSelectedVariant(product.id)?.price || product.minPrice) | currency:'VND':'symbol':'1.0-0' }}</strong>
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
    .card-rating { display:flex; align-items:center; gap:6px; text-decoration:none; margin:4px 0 2px; }
    .card-stars { display:inline-flex; color:#f59e0b; }
    .card-stars mat-icon { font-size:15px; width:15px; height:15px; }
    .card-rating small { color:#64748b; font-size:.74rem; font-weight:600; }
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

    /* Enhanced Search */
    .search-wrap { position: relative; }
    .search-box {
      display: flex; align-items: center; border: 2px solid #d9dfdc; border-radius: 12px;
      padding: 0 8px; transition: all 0.2s ease; background: #fff;
    }
    .search-box.focused { border-color: #b78a34; box-shadow: 0 0 0 3px rgba(183, 138, 52, 0.12); }
    .search-box input { border:0; outline:0; padding:0 7px; min-width:0; flex:1; height: 40px; }
    .search-prefix { color:#7a8784; font-size:20px; width:20px; height:20px; }

    .search-dropdown {
      position: absolute; top: calc(100% + 4px); left: 0; right: 0; z-index: 100;
      background: #fff; border: 1px solid #e2e8f0; border-radius: 12px;
      box-shadow: 0 8px 28px rgba(0,0,0,0.12); overflow: hidden; max-height: 360px; overflow-y: auto;
    }
    .dropdown-section { padding: 6px 0; }
    .dropdown-section + .dropdown-section { border-top: 1px solid #f1f5f9; }
    .dropdown-label { display:block; padding: 6px 14px 4px; font-size:0.72rem; font-weight:700; color:#94a3b8; letter-spacing:0.05em; text-transform:uppercase; }
    .dropdown-item {
      display: flex; align-items: center; gap: 10px; width: 100%; padding: 8px 14px; text-decoration: none;
      border: 0; background: transparent; cursor: pointer; font: inherit; font-size: 0.88rem;
      color: #334155; text-align: left; transition: background 0.12s ease;
    }
    .dropdown-item:hover { background: #f8fafc; }
    .dropdown-item mat-icon { font-size: 18px; width: 18px; height: 18px; color: #94a3b8; }
    .dropdown-item .highlight { background: #fef3c7; color: #b45309; font-weight: 700; border-radius: 2px; padding: 0 1px; }
    .suggest-thumb { width: 36px; height: 36px; border-radius: 6px; object-fit: cover; background: #f1f5f9; flex-shrink: 0; }
    .suggest-info { display: flex; flex-direction: column; min-width: 0; }
    .suggest-name { font-weight: 650; font-size: 0.85rem; color: #0f172a; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .suggest-price { font-size: 0.78rem; font-weight: 800; color: #173d38; }
    .clear-recent { color: #94a3b8; font-size: 0.82rem; }
    .clear-recent:hover { color: #ef4444; }

    /* Filter Chips */
    .filter-chips {
      display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 16px;
    }
    .chip {
      display: inline-flex; align-items: center; gap: 5px;
      background: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 20px;
      padding: 4px 8px 4px 11px; font-size: 0.8rem; font-weight: 650; color: #334155;
    }
    .chip mat-icon { font-size: 15px; width: 15px; height: 15px; color: #64748b; }
    .chip-remove {
      display: inline-flex; align-items: center; justify-content: center;
      width: 18px; height: 18px; border: 0; background: transparent; cursor: pointer;
      color: #94a3b8; border-radius: 50%; padding: 0;
    }
    .chip-remove:hover { background: #e2e8f0; color: #334155; }
    .chip-remove mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .chip-clear-all {
      border: 0; background: transparent; cursor: pointer; font: inherit;
      font-size: 0.78rem; font-weight: 650; color: #94a3b8; padding: 4px 8px;
    }
    .chip-clear-all:hover { color: #ef4444; }

    /* Highlight in grid */
    .product-name .highlight { background: #fef3c7; color: #b45309; font-weight: 800; border-radius: 2px; padding: 0 2px; }

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
  readonly recommendationPlacement = RecommendationPlacement;
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
  private carouselInterval: ReturnType<typeof setInterval> | undefined;

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

  // Enhanced search properties
  searchFocused = false;
  suggestions: SuggestResult[] = [];
  recentSearches: string[] = [];
  private searchSubject = new Subject<string>();

  get hasActiveFilters(): boolean {
    return !!(this.appliedSearch || this.categoryId || this.minPrice !== undefined || this.maxPrice !== undefined || this.inStock);
  }

  constructor(
    private productService: ProductService,
    private reviewService: ReviewService,
    private categoryService: CategoryService,
    private cartService: CartService,
    private wishlistService: WishlistService,
    private recentlyViewedService: RecentlyViewedService,
    private analyticsService: AnalyticsService,
    private authService: AuthService,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) { }

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

    // Debounced instant search with switchMap for autocomplete
    this.searchSubject.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      switchMap(query => {
        if (query.length >= 2) {
          return this.productService.suggestProducts(query).pipe(
            tap(res => this.suggestions = res?.data || [])
          );
        }
        this.suggestions = [];
        return [];
      }),
      takeUntil(this.destroy$)
    ).subscribe();

    // Load recent searches from localStorage
    this.loadRecentSearches();

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
        const t = s.trim();
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
    this.query = {
      ...this.query, q: this.appliedSearch || undefined, categoryId: this.categoryId,
      minPrice: this.minPrice, maxPrice: this.maxPrice, inStock: this.inStock
    };

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
        this.loadRatings();
      },
      error: () => this.errorMessage = 'Kết nối đến cửa hàng bị gián đoạn. Vui lòng thử lại.'
    });
  }

  private loadRatings(): void {
    const ids = this.products.map(p => p.id);
    if (!ids.length) {
      return;
    }
    this.reviewService.getProductRatings(ids).pipe(takeUntil(this.destroy$)).subscribe({
      next: res => {
        if (res.success && res.data) {
          const map = new Map(res.data.map(r => [r.productId, r]));
          this.products.forEach(p => {
            const r = map.get(p.id);
            if (r) {
              p.averageRating = r.averageRating;
              p.reviewCount = r.reviewCount;
            }
          });
        }
      },
      error: () => undefined
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
      this.analyticsService.track(
        AnalyticsEventType.Search,
        { query: this.appliedSearch },
        { source: AnalyticsEventSource.Search }
      );
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

  getCategoryName(id: number | undefined): string {
    const cat = this.categories?.find(c => c.id === id);
    return cat ? cat.name : '';
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
            productName: product.name
          }, {
            productId: product.id,
            variantId: targetVariantId,
            quantity: 1,
            source: AnalyticsEventSource.Catalog
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
            productName: product.name
          }, {
            productId: product.id,
            source: AnalyticsEventSource.Catalog
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
    this.router.navigate([], {
      relativeTo: this.route, replaceUrl: true, queryParams: {
        q: this.appliedSearch || null, category: this.categoryId ?? null, minPrice: this.minPrice ?? null,
        maxPrice: this.maxPrice ?? null, inStock: this.inStock || null, sort: this.sortValue,
        page: this.query.page || null
      }
    });
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

  // ───────── Enhanced Search Methods ─────────

  /** Highlight matching keyword in text for display */
  highlightMatch(text: string, query: string): string {
    if (!query || !text) return text || '';
    const escaped = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const regex = new RegExp(`(${escaped})`, 'gi');
    return text.replace(regex, '<span class="highlight">$1</span>');
  }

  onSearchFocus(): void {
    this.searchFocused = true;
    if (!this.draftSearch) {
      this.suggestions = [];
      this.loadRecentSearches();
    }
  }

  onSearchBlur(): void {
    // Delay hiding so clicks on dropdown register first
    setTimeout(() => this.searchFocused = false, 200);
  }

  onSearchInput(): void {
    this.searchSubject.next(this.draftSearch);
    // If search is cleared, trigger instant refresh
    if (!this.draftSearch && this.appliedSearch) {
      this.commitInstantSearch();
    }
  }

  /** Commits the current draft as an applied search (instant). */
  commitInstantSearch(): void {
    if (this.draftSearch.trim() !== this.appliedSearch) {
      this.saveRecentSearch(this.draftSearch.trim());
      this.applyFilters();
    }
  }

  selectSuggestion(suggestion: string): void {
    this.draftSearch = suggestion;
    this.searchFocused = false;
    this.commitInstantSearch();
  }

  clearSearch(): void {
    this.draftSearch = '';
    this.suggestions = [];
    if (this.appliedSearch) {
      this.applyFilters();
    }
    // Focus back to input
    const input = document.getElementById('catalog-search');
    input?.focus();
  }

  removeFilter(type: 'search' | 'category' | 'price' | 'inStock'): void {
    switch (type) {
      case 'search': this.draftSearch = ''; this.appliedSearch = ''; break;
      case 'category': this.categoryId = undefined; break;
      case 'price': this.minPrice = undefined; this.maxPrice = undefined; break;
      case 'inStock': this.inStock = false; break;
    }
    this.query.page = 0;
    this.syncUrl();
    this.loadProducts();
  }

  // ───────── Recent Searches (localStorage) ─────────

  private readonly RECENT_SEARCHES_KEY = 'marketplace_recent_searches';
  private readonly MAX_RECENT = 5;

  private loadRecentSearches(): void {
    try {
      const stored = localStorage.getItem(this.RECENT_SEARCHES_KEY);
      this.recentSearches = stored ? JSON.parse(stored) : [];
    } catch {
      this.recentSearches = [];
    }
  }

  private saveRecentSearch(query: string): void {
    if (!query) return;
    this.loadRecentSearches();
    this.recentSearches = this.recentSearches.filter(s => s !== query);
    this.recentSearches.unshift(query);
    if (this.recentSearches.length > this.MAX_RECENT) {
      this.recentSearches = this.recentSearches.slice(0, this.MAX_RECENT);
    }
    try {
      localStorage.setItem(this.RECENT_SEARCHES_KEY, JSON.stringify(this.recentSearches));
    } catch { /* ignore quota errors */ }
  }

  clearRecentSearches(): void {
    this.recentSearches = [];
    try {
      localStorage.removeItem(this.RECENT_SEARCHES_KEY);
    } catch { /* ignore */ }
  }
}
