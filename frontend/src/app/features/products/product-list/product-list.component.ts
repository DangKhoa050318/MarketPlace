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
import { ProductCatalogQuery, StorefrontProduct } from '../../../core/models/product.model';
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
    <section class="hero">
      <div>
        <span class="eyebrow">Curated technology · Ready to ship</span>
        <h1>Thiết bị tốt cho cách bạn sống và làm việc.</h1>
        <p>Hàng chính hãng, tồn kho minh bạch và giao nhanh từ hệ thống kho toàn quốc.</p>
      </div>
      <div class="hero-stat"><strong>{{ totalElements }}</strong><span>sản phẩm tuyển chọn</span></div>
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

    <section class="catalog-shell">
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
                    <img [src]="product.imageUrl || fallbackImage" (error)="useFallback($event)" [alt]="product.name">
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
                  <div class="meta"><span>{{ product.variantCount }} lựa chọn</span>
                    <span [class.out]="product.availableStock === 0">
                      {{ product.availableStock > 0 ? 'Còn hàng' : 'Tạm hết hàng' }}
                    </span>
                  </div>
                  <a class="product-name" [routerLink]="['/products', product.id]">{{ product.name }}</a>
                  <p>{{ product.description }}</p>
                  <div class="card-footer">
                    <div class="price"><small>Từ</small><strong>{{ product.minPrice | currency:'USD':'symbol':'1.0-0' }}</strong>
                      @if (product.maxPrice !== product.minPrice) {
                        <span>– {{ product.maxPrice | currency:'USD':'symbol':'1.0-0' }}</span>
                      }
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
    .hero { background:#102a2a; color:#fff; border-radius:24px; padding:38px 42px; display:flex;
      justify-content:space-between; align-items:end; gap:30px; margin-bottom:28px; overflow:hidden; }
    .eyebrow,.overline { color:#d9a441; font-size:.72rem; font-weight:800; letter-spacing:.14em; text-transform:uppercase; }
    .hero h1 { max-width:720px; font-size:clamp(2rem,4vw,3.5rem); line-height:1.04; letter-spacing:-.045em; margin:9px 0 12px; }
    .hero p { color:#c6d4d1; margin:0; max-width:640px; }
    .hero-stat { min-width:150px; border-left:1px solid #47615e; padding-left:24px; display:flex; flex-direction:column; }
    .hero-stat strong { color:#f0c36a; font-size:2.4rem; }.hero-stat span { color:#c6d4d1; font-size:.8rem; }
    
    .recently-viewed-section {
      display: flex; flex-direction: column; gap: 14px; padding: 18px; margin-bottom: 24px;
      background: #fff; border: 1px solid #e4e7e5; border-radius: 16px;
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

    .catalog-shell { display:grid; grid-template-columns:260px minmax(0,1fr); gap:30px; align-items:start; }
    .filters { position:sticky; top:94px; background:#fff; border:1px solid #e4e7e5; padding:22px; border-radius:16px; }
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
    .sort-control select { width:190px; }.product-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:18px; }
    .product-card { background:#fff; border:1px solid #e4e7e5; border-radius:15px; overflow:hidden; transition:.2s ease; min-width:0; display:flex; flex-direction:column; }
    .product-card:hover { transform:translateY(-3px); border-color:#c7b17f; box-shadow:0 16px 35px rgba(24,45,42,.09); }
    .image-box { position:relative; width:100%; aspect-ratio:4/3; }
    .image-link { position:relative; display:block; background:#eef1ef; width:100%; height:100%; overflow:hidden; }
    .image-link img { width:100%; height:100%; object-fit:cover; transition:transform .35s; }.product-card:hover img { transform:scale(1.035); }
    .category-badge,.scarcity-badge { position:absolute; top:12px; left:12px; background:rgba(255,255,255,.94); color:#34413e; padding:6px 9px;
      border-radius:7px; font-size:.7rem; font-weight:800; }.scarcity-badge { left:auto; right:12px; background:#fff3d8; color:#8a5b08; }
    .wishlist-btn {
      position: absolute; top: 10px; right: 10px; z-index: 2;
      background: rgba(255, 255, 255, 0.92); color: #64748b;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.14); width: 36px; height: 36px; line-height: 36px;
    }
    .wishlist-btn.active { color: #e11d48; background: #fff1f2; }
    .card-body { padding:17px; display:flex; flex-direction:column; flex:1; }.meta { display:flex; justify-content:space-between; color:#24705f; font-size:.7rem; font-weight:750; }
    .meta .out { color:#a33a32; }.product-name { color:#17202a; font-size:1.04rem; line-height:1.3; font-weight:800; text-decoration:none;
      display:block; margin:10px 0 6px; }.card-body>p { color:#687572; font-size:.8rem; line-height:1.5; height:2.4em; overflow:hidden; margin:0 0 18px; }
    .card-footer { border-top:1px solid #edf0ee; padding-top:14px; margin-top:auto; display:flex; align-items:end; justify-content:space-between; gap:8px; }
    .price { display:flex; flex-wrap:wrap; align-items:baseline; gap:4px; }.price small { width:100%; color:#7a8784; }.price strong { font-size:1.25rem; color:#173d38; }
    .price span { font-size:.72rem; color:#7a8784; }.card-footer button { background:#173d38!important; color:#fff!important; min-width:86px; }
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
  products: StorefrontProduct[] = [];
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
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
    this.addingId = product.id;
    this.productService.getVariants(product.id).pipe(
      takeUntil(this.destroy$), finalize(() => this.addingId = undefined)
    ).subscribe({
      next: response => {
        const variant = response.data.find(item => item.active);
        if (!variant) { this.notification.error('Sản phẩm chưa có lựa chọn khả dụng'); return; }
        this.cartService.addToCart(variant.id, 1).pipe(takeUntil(this.destroy$)).subscribe({
          next: (res) => {
            if (res.success) {
              this.analyticsService.track(AnalyticsEventType.AddToCart, {
                productId: product.id,
                productName: product.name,
                variantId: variant.id,
                quantity: 1
              });
              this.notification.success(`Đã thêm “${product.name}” vào giỏ`);
            } else {
              this.notification.error(res.message || 'Không thể thêm vào giỏ hàng');
            }
          },
          error: error => this.notification.error(error.error?.message || 'Không thể thêm vào giỏ hàng')
        });
      },
      error: () => this.notification.error('Không thể tải lựa chọn sản phẩm')
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
