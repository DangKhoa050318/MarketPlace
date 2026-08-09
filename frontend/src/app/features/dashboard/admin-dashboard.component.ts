import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import {
  AnalyticsFilters,
  AnalyticsOverview,
  AnalyticsExportJob,
  AnalyticsExportType,
  DashboardService,
  DashboardStats,
  FunnelSummary,
  ProductPerformance,
  PromotionPerformance,
  PromotionTrendPoint
} from '../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatIconModule, MatButtonModule],
  template: `
    <section class="dashboard">
      <header class="page-header">
        <div>
          <span class="eyebrow">Operations Command Center</span>
          <h1 class="page-title">
            <mat-icon class="title-icon">space_dashboard</mat-icon> Executive Dashboard
          </h1>
          <p class="page-subtitle">Monitor real-time sales metrics, revenue analytics, inventory health and active users</p>
        </div>

        <div class="header-actions">
          <span class="updated-at" *ngIf="lastUpdated">
            <mat-icon>schedule</mat-icon>
            Updated {{ lastUpdated | date:'HH:mm:ss' }}
          </span>
          <button class="refresh-button btn-solid-primary" type="button" (click)="loadStats()" [disabled]="loading">
            <mat-icon [class.spinning]="loading">refresh</mat-icon>
            {{ loading ? 'Refreshing' : 'Refresh data' }}
          </button>
        </div>
      </header>

      <section class="analytics-toolbar" aria-label="Analytics date range">
        <div class="range-presets" role="group" aria-label="Quick date ranges">
          <button type="button" *ngFor="let days of rangePresets"
                  [class.active]="selectedRangeDays === days" (click)="setRange(days)">
            {{ days }} days
          </button>
        </div>
        <label>
          <span>From</span>
          <input type="date" [(ngModel)]="fromDate" [max]="toDate">
        </label>
        <label>
          <span>To</span>
          <input type="date" [(ngModel)]="toDate" [min]="fromDate">
        </label>
        <button type="button" class="apply-range" (click)="applyCustomRange()"
                [disabled]="loading || !isDateRangeValid">
          <mat-icon>calendar_month</mat-icon>
          Apply
        </button>
      </section>

      <section class="analytics-filters" aria-label="Analytics filters">
        <label><span>Category ID</span><input type="number" min="1" [(ngModel)]="filterCategoryId"></label>
        <label><span>Product ID</span><input type="number" min="1" [(ngModel)]="filterProductId"></label>
        <label><span>Campaign</span><input type="search" [(ngModel)]="filterCampaign" placeholder="Campaign name"></label>
        <label><span>Placement</span>
          <select [(ngModel)]="filterPlacement">
            <option value="">All placements</option>
            <option value="CAMPAIGN_STRIP">Campaign strip</option>
            <option value="HOME_BEST_SELLERS">Home best sellers</option>
            <option value="PRODUCT_DETAIL_SIMILAR">Product detail similar</option>
            <option value="CART_CROSS_SELL">Cart cross-sell</option>
          </select>
        </label>
        <label><span>Device</span>
          <select [(ngModel)]="filterDeviceType">
            <option value="">All devices</option>
            <option value="desktop">Desktop</option>
            <option value="tablet">Tablet</option>
            <option value="mobile">Mobile</option>
          </select>
        </label>
        <button type="button" class="clear-filters" (click)="clearFilters()" [disabled]="loading">Clear</button>
      </section>

      <div *ngIf="errorMessage" class="error-banner surface-card" role="alert">
        <mat-icon>error_outline</mat-icon>
        <div>
          <strong>Dashboard data is unavailable</strong>
          <span>{{ errorMessage }}</span>
        </div>
        <button type="button" (click)="loadStats()">Try again</button>
      </div>

      <div *ngIf="loading && !stats" class="loading-grid" aria-label="Loading dashboard">
        <div class="skeleton surface-card" *ngFor="let item of skeletonItems"></div>
      </div>

      <ng-container *ngIf="stats as data">
        <div class="analytics-status analytics-loading" *ngIf="analyticsLoading" aria-live="polite">
          <span class="analytics-skeleton" *ngFor="let item of analyticsSkeletonItems"></span>
          <span class="visually-hidden">Loading analytics overview</span>
        </div>

        <div class="analytics-status analytics-error" *ngIf="analyticsError" role="alert">
          <mat-icon>query_stats</mat-icon>
          <div>
            <strong>Analytics data is unavailable</strong>
            <span>{{ analyticsError }}</span>
          </div>
          <button type="button" (click)="loadAnalytics()">Try again</button>
        </div>

        <div class="analytics-status analytics-empty"
             *ngIf="!analyticsLoading && !analyticsError && analyticsEmpty">
          <mat-icon>insights</mat-icon>
          <div>
            <strong>No journey activity in this period</strong>
            <span>Choose a wider date range to review storefront conversion data.</span>
          </div>
        </div>

        <div class="stale-notice" *ngIf="!analyticsLoading && !analyticsError && analyticsStale" role="status">
          <mat-icon>history</mat-icon>
          Analytics data may be stale. Refresh to request the latest aggregate.
        </div>

        <section class="analytics-section" *ngIf="!analyticsLoading && analyticsOverview as overview">
          <ng-container *ngIf="!analyticsEmpty">
          <div class="section-heading">
            <div>
              <span class="panel-label">Journey Analytics</span>
              <h2>Conversion Overview</h2>
            </div>
            <span class="updated-at" *ngIf="overview.lastUpdatedAt">
              <mat-icon>schedule</mat-icon>
              Data updated {{ overview.lastUpdatedAt | date:'medium' }}
            </span>
          </div>
          <div class="analytics-kpi-grid">
            <article class="analytics-kpi">
              <span>Product views</span>
              <strong>{{ overview.productViews | number }}</strong>
              <small>Storefront detail views</small>
            </article>
            <article class="analytics-kpi">
              <span>Add-to-cart rate</span>
              <strong>{{ overview.addToCartRate | percent:'1.0-1' }}</strong>
              <small>{{ overview.addToCarts | number }} cart additions</small>
            </article>
            <article class="analytics-kpi">
              <span>Checkout rate</span>
              <strong>{{ overview.checkoutRate | percent:'1.0-1' }}</strong>
              <small>{{ overview.beginCheckouts | number }} checkouts started</small>
            </article>
            <article class="analytics-kpi">
              <span>Order conversion</span>
              <strong>{{ overview.orderConversionRate | percent:'1.0-1' }}</strong>
              <small>{{ overview.orders | number }} attributed orders</small>
            </article>
            <article class="analytics-kpi">
              <span>Returning customers</span>
              <strong>{{ overview.returningCustomerRate | percent:'1.0-1' }}</strong>
              <small>Customers with repeat orders</small>
            </article>
          </div>
          </ng-container>
        </section>

        <div class="kpi-grid">
          <article class="kpi-card surface-card surface-card-hover">
            <div class="kpi-top">
              <span class="icon revenue"><mat-icon>payments</mat-icon></span>
              <span class="context">Confirmed revenue</span>
            </div>
            <span class="label">Total Gross Revenue</span>
            <strong class="kpi-amount revenue-text">{{ data.totalRevenue | currency:'VND':'symbol':'1.0-0' }}</strong>
            <small>Recognized across {{ data.totalOrders | number }} orders</small>
          </article>

          <a class="kpi-card surface-card surface-card-hover clickable-card" [routerLink]="['/admin/orders']">
            <div class="kpi-top">
              <span class="icon orders"><mat-icon>receipt_long</mat-icon></span>
              <span class="context">{{ pendingRate | number:'1.0-1' }}% pending</span>
            </div>
            <span class="label">Total Orders Processed</span>
            <strong class="kpi-amount">{{ data.totalOrders | number }}</strong>
            <small>{{ data.pendingOrders | number }} orders require fulfillment</small>
          </a>

          <article class="kpi-card surface-card surface-card-hover">
            <div class="kpi-top">
              <span class="icon average"><mat-icon>analytics</mat-icon></span>
              <span class="context">Average per transaction</span>
            </div>
            <span class="label">Average Order Value</span>
            <strong class="kpi-amount">{{ averageOrderValue | currency:'VND':'symbol':'1.0-0' }}</strong>
            <small>Calculated from customer checkout data</small>
          </article>

          <a class="kpi-card surface-card surface-card-hover clickable-card" [routerLink]="['/admin/orders']" [queryParams]="{ status: 'DELIVERED' }">
            <div class="kpi-top">
              <span class="icon delivery"><mat-icon>task_alt</mat-icon></span>
              <span class="context">{{ fulfillmentRate | number:'1.0-1' }}% fulfillment</span>
            </div>
            <span class="label">Delivered Orders</span>
            <strong class="kpi-amount">{{ data.completedOrders | number }}</strong>
            <small>Completed full delivery lifecycle</small>
          </a>

          <a class="kpi-card surface-card surface-card-hover clickable-card" routerLink="/admin/products">
            <div class="kpi-top">
              <span class="icon products"><mat-icon>inventory_2</mat-icon></span>
              <span class="context">Catalog items</span>
            </div>
            <span class="label">Catalog Products</span>
            <strong class="kpi-amount">{{ data.totalProducts | number }}</strong>
            <small>Active products listed on storefront</small>
          </a>

          <a class="kpi-card surface-card surface-card-hover clickable-card" routerLink="/admin/users">
            <div class="kpi-top">
              <span class="icon customers"><mat-icon>group</mat-icon></span>
              <span class="context">Registered accounts</span>
            </div>
            <span class="label">Registered Customers</span>
            <strong class="kpi-amount">{{ data.totalCustomers | number }}</strong>
            <small>Active user accounts in system</small>
          </a>
        </div>

        <div class="content-grid">
          <section class="panel surface-card workload-panel">
            <div class="panel-heading">
              <div>
                <span class="panel-label">Order Fulfillment</span>
                <h2>Fulfillment Pipeline</h2>
              </div>
              <a routerLink="/admin/orders" class="view-all-link">
                Manage orders <mat-icon>arrow_forward</mat-icon>
              </a>
            </div>

            <a class="workload-row clickable-row" [routerLink]="['/admin/orders']" [queryParams]="{ status: 'PENDING' }">
              <div class="workload-copy">
                <span>Pending Queue</span>
                <strong>{{ data.pendingOrders | number }}</strong>
              </div>
              <div class="progress-track" aria-label="Pending order percentage">
                <span class="pending-progress" [style.width.%]="pendingRate"></span>
              </div>
              <span class="percentage">{{ pendingRate | number:'1.0-1' }}%</span>
            </a>

            <a class="workload-row clickable-row" [routerLink]="['/admin/orders']" [queryParams]="{ status: 'DELIVERED' }">
              <div class="workload-copy">
                <span>Delivered</span>
                <strong>{{ data.completedOrders | number }}</strong>
              </div>
              <div class="progress-track" aria-label="Delivered order percentage">
                <span class="delivered-progress" [style.width.%]="fulfillmentRate"></span>
              </div>
              <span class="percentage">{{ fulfillmentRate | number:'1.0-1' }}%</span>
            </a>

            <a class="attention clickable-attention" [class.clear]="data.pendingOrders === 0" [routerLink]="['/admin/orders']" [queryParams]="{ status: 'PENDING' }">
              <mat-icon>{{ data.pendingOrders > 0 ? 'notification_important' : 'check_circle' }}</mat-icon>
              <div>
                <strong>{{ data.pendingOrders > 0 ? 'Action Needed: Pending Orders' : 'Fulfillment Status Operational' }}</strong>
                <span *ngIf="data.pendingOrders > 0">
                  {{ data.pendingOrders }} order{{ data.pendingOrders === 1 ? '' : 's' }} awaiting admin status update. Click to view pending queue.
                </span>
                <span *ngIf="data.pendingOrders === 0">No orders currently pending review.</span>
              </div>
            </a>
          </section>

          <aside class="panel surface-card quick-actions">
            <div class="panel-heading">
              <div>
                <span class="panel-label">Quick Actions</span>
                <h2>Shortcuts</h2>
              </div>
            </div>
            
            <a routerLink="/admin/orders" [queryParams]="{ status: 'PENDING' }" class="shortcut-item">
              <span class="action-icon order-icon"><mat-icon>receipt_long</mat-icon></span>
              <div class="shortcut-text">
                <strong>Process Pending Orders</strong>
                <small>Review & update order transitions</small>
              </div>
              <mat-icon class="arrow-icon">chevron_right</mat-icon>
            </a>

            <a routerLink="/admin/products" class="shortcut-item">
              <span class="action-icon product-icon"><mat-icon>inventory_2</mat-icon></span>
              <div class="shortcut-text">
                <strong>Catalog Management</strong>
                <small>Review inventory, stock & prices</small>
              </div>
              <mat-icon class="arrow-icon">chevron_right</mat-icon>
            </a>

            <a routerLink="/admin/products/new" class="shortcut-item">
              <span class="action-icon add-icon"><mat-icon>add_box</mat-icon></span>
              <div class="shortcut-text">
                <strong>Add New Product</strong>
                <small>Create a new catalog item</small>
              </div>
              <mat-icon class="arrow-icon">chevron_right</mat-icon>
            </a>

            <a routerLink="/admin/users" class="shortcut-item">
              <span class="action-icon user-icon"><mat-icon>manage_accounts</mat-icon></span>
              <div class="shortcut-text">
                <strong>User Management</strong>
                <small>Manage roles & account statuses</small>
              </div>
              <mat-icon class="arrow-icon">chevron_right</mat-icon>
            </a>
          </aside>
        </div>

        <section class="panel surface-card funnel-panel" *ngIf="!analyticsEmpty && funnel as summary"
                 aria-labelledby="funnel-title" aria-describedby="funnel-description">
          <div class="panel-heading">
            <div>
              <span class="panel-label">Customer Journey</span>
              <h2 id="funnel-title">Storefront Funnel</h2>
              <p id="funnel-description" class="visually-hidden">
                Product journey counts, conversion rates and drop-off rates for the selected date range.
              </p>
            </div>
          </div>
          <div class="funnel-grid" role="list">
            <article class="funnel-step" role="listitem" *ngFor="let step of summary.steps; let i = index"
                     [attr.aria-label]="step.step + ': ' + step.count + ' events'">
              <span>{{ step.step }}</span>
              <strong>{{ step.count | number }}</strong>
              <div class="progress-track" role="progressbar" aria-valuemin="0" aria-valuemax="100"
                   [attr.aria-valuenow]="step.conversionRate * 100"
                   [attr.aria-label]="step.step + ' conversion rate'">
                <span [style.width.%]="step.conversionRate * 100"></span>
              </div>
              <small>
                {{ i === 0 ? 'Entry' : (step.conversionRate | percent:'1.0-1') + ' conversion' }}
                <ng-container *ngIf="i > 0"> - {{ step.dropOffRate | percent:'1.0-1' }} drop-off</ng-container>
              </small>
            </article>
          </div>
        </section>

        <section class="panel surface-card performance-panel" aria-labelledby="product-performance-title">
          <div class="panel-heading performance-heading">
            <div>
              <span class="panel-label">Catalog Analytics</span>
              <h2 id="product-performance-title">Product Performance</h2>
            </div>
            <div class="table-controls">
              <label>
                <span class="visually-hidden">Search products</span>
                <input type="search" [(ngModel)]="productSearch" placeholder="Search product name"
                       (keyup.enter)="applyProductSearch()">
              </label>
              <button type="button" (click)="applyProductSearch()" aria-label="Search product performance">
                <mat-icon>search</mat-icon>
              </button>
            </div>
          </div>
          <div class="table-loading" *ngIf="productsLoading">Loading product metrics...</div>
          <div class="table-error" *ngIf="!productsLoading && productsError" role="alert">
            <span>{{ productsError }}</span>
            <button type="button" (click)="loadProducts()">Try again</button>
          </div>
          <div class="table-wrap" *ngIf="!productsLoading && productRows.length">
            <table>
              <thead><tr>
                <th><button type="button" (click)="sortProducts('productName')">Product <mat-icon>{{ sortIcon('productName') }}</mat-icon></button></th>
                <th><button type="button" (click)="sortProducts('productViews')">Views <mat-icon>{{ sortIcon('productViews') }}</mat-icon></button></th>
                <th><button type="button" (click)="sortProducts('wishlists')">Wishlist <mat-icon>{{ sortIcon('wishlists') }}</mat-icon></button></th>
                <th><button type="button" (click)="sortProducts('addToCarts')">Cart <mat-icon>{{ sortIcon('addToCarts') }}</mat-icon></button></th>
                <th><button type="button" (click)="sortProducts('orders')">Orders <mat-icon>{{ sortIcon('orders') }}</mat-icon></button></th>
                <th><button type="button" (click)="sortProducts('averageRating')">Rating <mat-icon>{{ sortIcon('averageRating') }}</mat-icon></button></th>
                <th><button type="button" (click)="sortProducts('questionCount')">Questions <mat-icon>{{ sortIcon('questionCount') }}</mat-icon></button></th>
              </tr></thead>
              <tbody>
                <tr *ngFor="let row of productRows">
                  <td><a [routerLink]="['/products', row.productId]">{{ row.productName }}</a></td>
                  <td>{{ row.productViews | number }}</td><td>{{ row.wishlists | number }}</td>
                  <td>{{ row.addToCarts | number }}</td><td>{{ row.orders | number }}</td>
                  <td>{{ row.averageRating | number:'1.1-1' }}</td><td>{{ row.questionCount | number }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="table-empty" *ngIf="!productsLoading && !productsError && !productRows.length">No products match this filter.</div>
          <div class="table-pagination">
            <span>{{ productTotal | number }} products</span>
            <button type="button" (click)="changeProductPage(-1)" [disabled]="productPage === 0" aria-label="Previous product page"><mat-icon>chevron_left</mat-icon></button>
            <span>Page {{ productPage + 1 }} of {{ productTotalPages || 1 }}</span>
            <button type="button" (click)="changeProductPage(1)" [disabled]="productPage + 1 >= productTotalPages" aria-label="Next product page"><mat-icon>chevron_right</mat-icon></button>
          </div>
        </section>

        <section class="panel surface-card campaign-panel" aria-labelledby="campaign-title">
          <div class="panel-heading">
            <div><span class="panel-label">Attribution</span><h2 id="campaign-title">Campaign & Placement Performance</h2></div>
          </div>
          <p class="chart-description">Horizontal bars compare reach and engagement; CTR shows click efficiency.</p>
          <div class="trend-chart" *ngIf="promotionTrend.length" role="img" aria-label="Daily recommendation impressions and clicks">
            <div class="trend-day" *ngFor="let point of promotionTrend" [attr.aria-label]="point.date + ': ' + point.impressions + ' impressions and ' + point.clicks + ' clicks'">
              <div class="trend-bars"><i class="trend-impressions" [style.height.%]="trendHeight(point.impressions)"></i><i class="trend-clicks" [style.height.%]="trendHeight(point.clicks)"></i></div>
              <span>{{ point.date | date:'MMM d' }}</span>
            </div>
          </div>
          <div class="trend-legend" *ngIf="promotionTrend.length"><span><i class="legend-impressions"></i>Impressions</span><span><i class="legend-clicks"></i>Clicks</span></div>
          <div class="campaign-grid" *ngIf="promotionRows.length; else noCampaigns">
            <article class="campaign-row" *ngFor="let row of promotionRows">
              <div class="campaign-copy"><strong>{{ row.campaign || 'Organic recommendation' }}</strong><span>{{ row.placement || 'Unassigned' }} · {{ row.strategy || 'Default' }}</span></div>
              <div class="metric-bars" role="img" [attr.aria-label]="campaignAria(row)">
                <div><span>Impressions</span><i class="bar impressions" [style.width.%]="barWidth(row.impressions)"></i><b>{{ row.impressions }}</b></div>
                <div><span>Clicks</span><i class="bar clicks" [style.width.%]="barWidth(row.clicks)"></i><b>{{ row.clicks }}</b></div>
                <div><span>Cart</span><i class="bar carts" [style.width.%]="barWidth(row.addToCarts)"></i><b>{{ row.addToCarts }}</b></div>
                <div><span>Orders</span><i class="bar orders-bar" [style.width.%]="barWidth(row.attributedOrders)"></i><b>{{ row.attributedOrders }}</b></div>
              </div>
              <div class="ctr"><span>CTR</span><strong>{{ row.clickThroughRate | percent:'1.0-1' }}</strong></div>
            </article>
          </div>
          <ng-template #noCampaigns><div class="table-empty">No attributed campaign activity in this period.</div></ng-template>
        </section>

        <section class="panel surface-card export-panel" *ngIf="authService.getRole() === 'ADMIN'" aria-labelledby="export-title">
          <div><span class="panel-label">Reporting</span><h2 id="export-title">Export Center</h2></div>
          <div class="export-controls">
            <select [(ngModel)]="exportType" aria-label="Export report type">
              <option value="OVERVIEW">Overview KPI</option><option value="PRODUCT_PERFORMANCE">Product performance</option><option value="PROMOTION_RECOMMENDATION">Campaign attribution</option>
            </select>
            <button type="button" (click)="createExport()" [disabled]="exportBusy"><mat-icon>download</mat-icon>{{ exportBusy ? 'Creating...' : 'Create CSV' }}</button>
          </div>
          <div class="export-status" *ngIf="exportJob" role="status">
            <mat-icon>{{ exportJob.status === 'COMPLETED' ? 'check_circle' : exportJob.status === 'FAILED' ? 'error' : 'sync' }}</mat-icon>
            <div><strong>{{ exportJob.status }}</strong><span *ngIf="exportJob.expiresAt">Download expires {{ exportJob.expiresAt | date:'medium' }}</span><span *ngIf="exportJob.errorMessage">{{ exportJob.errorMessage }}</span></div>
            <button type="button" *ngIf="exportJob.status === 'COMPLETED'" (click)="downloadExport()">Download CSV</button>
          </div>
          <div class="export-error" *ngIf="exportError" role="alert">{{ exportError }}</div>
        </section>
      </ng-container>
    </section>
  `,
  styles: [`
    :host { display: block; color: var(--text-main); }
    .dashboard { display: grid; gap: 24px; max-width: 1440px; margin: 0 auto; }

    .page-header {
      display: flex; align-items: flex-end; justify-content: space-between; gap: 24px;
      padding: 4px 0 16px; border-bottom: 1px solid var(--border-subtle);
    }
    .eyebrow, .panel-label {
      color: #0284c7; font-size: .7rem; font-weight: 800;
      letter-spacing: .13em; text-transform: uppercase;
    }
    .page-title {
      display: flex; align-items: center; gap: 10px;
      margin: 4px 0 0 0; font-size: 2rem; font-weight: 800; letter-spacing: -.03em;
      color: var(--text-main);
    }
    .title-icon { font-size: 32px; width: 32px; height: 32px; color: #4f46e5; }
    .page-subtitle { margin: 4px 0 0 0; color: var(--text-muted); font-size: .9rem; }
    .header-actions { display: flex; align-items: center; gap: 14px; }
    .updated-at { display: flex; align-items: center; gap: 6px; color: var(--text-muted); font-size: .75rem; }
    .updated-at mat-icon { width: 16px; height: 16px; font-size: 16px; }

    .analytics-toolbar {
      display: flex; align-items: flex-end; gap: 12px; padding: 14px 0;
      border-bottom: 1px solid var(--border-subtle);
    }
    .range-presets { display: flex; gap: 4px; padding: 3px; border: 1px solid #cbd5e1; border-radius: 6px; }
    .range-presets button {
      min-height: 34px; padding: 0 12px; border: 0; border-radius: 4px;
      color: var(--text-secondary); background: transparent; font: inherit; font-size: .78rem; cursor: pointer;
    }
    .range-presets button.active { color: #ffffff; background: #0369a1; font-weight: 700; }
    .analytics-toolbar label { display: grid; gap: 4px; color: var(--text-secondary); font-size: .7rem; font-weight: 700; }
    .analytics-toolbar input {
      min-height: 36px; padding: 0 10px; border: 1px solid #cbd5e1; border-radius: 6px;
      color: var(--text-main); background: #ffffff; font: inherit; font-size: .78rem;
    }
    .apply-range {
      display: inline-flex; align-items: center; gap: 6px; min-height: 38px; padding: 0 14px;
      border: 0; border-radius: 6px; color: #ffffff; background: #0369a1; font: inherit;
      font-size: .78rem; font-weight: 700; cursor: pointer;
    }
    .apply-range:disabled { opacity: .5; cursor: not-allowed; }
    .apply-range mat-icon { width: 18px; height: 18px; font-size: 18px; }
    .analytics-filters {
      display: grid; grid-template-columns: repeat(5, minmax(130px, 1fr)) auto;
      align-items: end; gap: 10px;
    }
    .analytics-filters label { display: grid; gap: 4px; color: var(--text-secondary); font-size: .7rem; font-weight: 700; }
    .analytics-filters input, .analytics-filters select {
      min-width: 0; min-height: 36px; padding: 0 10px; box-sizing: border-box;
      border: 1px solid #cbd5e1; border-radius: 6px; color: var(--text-main); background: #fff;
      font: inherit; font-size: .78rem;
    }
    .clear-filters {
      min-height: 36px; padding: 0 12px; border: 1px solid #cbd5e1; border-radius: 6px;
      color: #0369a1; background: #fff; font: inherit; font-size: .78rem; font-weight: 700; cursor: pointer;
    }
    .clear-filters:disabled { opacity: .5; cursor: not-allowed; }

    .analytics-section { display: grid; gap: 14px; }
    .analytics-status {
      display: flex; align-items: center; gap: 12px; min-height: 88px; padding: 18px;
      box-sizing: border-box; border: 1px solid #dbe4ee; border-radius: 6px; background: #ffffff;
    }
    .analytics-status div { flex: 1; }
    .analytics-status strong, .analytics-status span { display: block; }
    .analytics-status strong { font-size: .84rem; }
    .analytics-status span { margin-top: 3px; color: var(--text-muted); font-size: .74rem; }
    .analytics-status button {
      border: 0; color: #0369a1; background: transparent; font: inherit; font-size: .78rem;
      font-weight: 700; cursor: pointer;
    }
    .analytics-loading { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; padding: 0; border: 0; }
    .analytics-skeleton {
      min-height: 118px; margin: 0 !important; border-radius: 6px;
      background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%);
      background-size: 200% 100%; animation: shimmer 1.2s infinite;
    }
    .analytics-error { border-color: #fecaca; color: #b91c1c; background: #fef2f2; }
    .analytics-empty { color: #475569; background: #f8fafc; }
    .stale-notice {
      display: flex; align-items: center; gap: 8px; padding: 10px 12px; border: 1px solid #fde68a;
      border-radius: 6px; color: #92400e; background: #fffbeb; font-size: .74rem; font-weight: 650;
    }
    .stale-notice mat-icon { width: 18px; height: 18px; font-size: 18px; }
    .visually-hidden {
      position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px;
      overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0;
    }
    .section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
    .section-heading h2 { margin: 4px 0 0; font-size: 1.1rem; }
    .analytics-kpi-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; }
    .analytics-kpi {
      display: grid; gap: 7px; min-height: 118px; padding: 18px; box-sizing: border-box;
      border: 1px solid #dbe4ee; border-radius: 6px; background: #ffffff;
    }
    .analytics-kpi span { color: var(--text-secondary); font-size: .76rem; font-weight: 700; }
    .analytics-kpi strong { color: var(--text-main); font-size: 1.45rem; }
    .analytics-kpi small { color: var(--text-muted); font-size: .7rem; }
    
    .refresh-button {
      display: flex; align-items: center; gap: 8px; min-height: 40px; padding: 0 16px;
      border: 0; font: inherit; font-size: .8rem; font-weight: 700; cursor: pointer;
    }
    .refresh-button:disabled { opacity: .55; cursor: wait; }
    .refresh-button mat-icon { width: 18px; height: 18px; font-size: 18px; }
    .spinning { animation: spin .8s linear infinite; }

    .kpi-grid, .loading-grid {
      display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px;
    }
    .kpi-card {
      display: flex; flex-direction: column; justify-content: space-between;
      min-height: 150px; padding: 22px; box-sizing: border-box;
      background: #ffffff;
    }
    .clickable-card { cursor: pointer; }
    .clickable-card:hover { border-color: var(--primary) !important; }
    .clickable-card, .clickable-row, .clickable-attention { color: inherit; text-decoration: none; }
    a:focus-visible, button:focus-visible, input:focus-visible {
      outline: 3px solid #0ea5e9; outline-offset: 3px;
    }

    .kpi-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
    .icon {
      display: grid; width: 40px; height: 40px; place-items: center; border-radius: 10px;
    }
    .icon mat-icon { width: 22px; height: 22px; font-size: 22px; }
    .revenue { color: #10b981; background: #ecfdf5; }
    .orders { color: #4f46e5; background: #eef2ff; }
    .average { color: #8b5cf6; background: #f3e8ff; }
    .delivery { color: #0284c7; background: #e0f2fe; }
    .products { color: #f59e0b; background: #fffbeb; }
    .customers { color: #ec4899; background: #fce7f3; }
    
    .context { color: var(--text-muted); font-size: .7rem; font-weight: 600; }
    .label { color: var(--text-secondary); font-size: .8rem; font-weight: 650; }
    .kpi-amount { margin: 4px 0; color: var(--text-main); font-size: 1.7rem; font-weight: 800; letter-spacing: -.03em; }
    .revenue-text { color: #10b981 !important; }
    .kpi-card > small { color: var(--text-muted); font-size: .72rem; }

    .content-grid { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(320px, 0.8fr); gap: 18px; }
    .panel {
      padding: 24px; box-sizing: border-box; background: #ffffff;
    }
    .panel-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 22px; }
    .panel h2 { margin: 4px 0 0; color: var(--text-main); font-size: 1.1rem; font-weight: 800; }
    .view-all-link {
      display: flex; align-items: center; gap: 5px; color: #0284c7;
      font-size: .78rem; font-weight: 700; text-decoration: none;
    }
    .view-all-link mat-icon { width: 16px; height: 16px; font-size: 16px; }

    .workload-row {
      display: grid; grid-template-columns: 140px minmax(120px, 1fr) 52px;
      align-items: center; gap: 16px; margin: 18px 0;
    }
    .clickable-row { cursor: pointer; padding: 6px 8px; border-radius: 8px; transition: background-color 0.15s ease; }
    .clickable-row:hover { background-color: #f1f5f9; }
    .workload-copy { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; }
    .workload-copy span { color: var(--text-secondary); font-size: .78rem; font-weight: 600; }
    .workload-copy strong { color: var(--text-main); font-size: .95rem; font-weight: 800; }
    .progress-track { height: 8px; overflow: hidden; border-radius: 999px; background: #e2e8f0; }
    .progress-track span { display: block; height: 100%; border-radius: inherit; transition: width .3s ease; }
    .pending-progress { background: #f59e0b; }
    .delivered-progress { background: #10b981; }
    .percentage { color: var(--text-secondary); font-size: .75rem; font-weight: 700; text-align: right; }
    
    .attention {
      display: flex; align-items: center; gap: 14px; margin-top: 24px; padding: 16px;
      border: 1px solid rgba(245, 158, 11, .3); border-radius: 10px; background: #fffbeb;
    }
    .clickable-attention { cursor: pointer; transition: transform 0.15s ease, box-shadow 0.15s ease; }
    .clickable-attention:hover { transform: translateY(-1px); box-shadow: var(--shadow-sm); }
    .attention mat-icon { color: #f59e0b; font-size: 24px; width: 24px; height: 24px; }
    .attention strong, .attention span { display: block; }
    .attention strong { color: #b45309; font-size: .82rem; }
    .attention span { margin-top: 3px; color: var(--text-secondary); font-size: .74rem; }
    .attention.clear { border-color: rgba(16, 185, 129, .3); background: #ecfdf5; }
    .attention.clear mat-icon, .attention.clear strong { color: #047857; }

    .quick-actions { display: flex; flex-direction: column; }
    .shortcut-item {
      display: grid; grid-template-columns: 40px 1fr 20px; align-items: center; gap: 14px;
      padding: 12px 10px; border-radius: 10px; color: inherit; text-decoration: none;
      transition: background-color 0.15s ease;
    }
    .shortcut-item:hover { background: #f1f5f9; }
    .shortcut-item:hover strong { color: #4f46e5; }
    
    .action-icon {
      display: grid; width: 40px; height: 40px; place-items: center; border-radius: 10px;
    }
    .action-icon mat-icon { width: 20px; height: 20px; font-size: 20px; }
    .order-icon { color: #4f46e5; background: #eef2ff; }
    .product-icon { color: #0284c7; background: #e0f2fe; }
    .add-icon { color: #10b981; background: #ecfdf5; }
    .user-icon { color: #8b5cf6; background: #f3e8ff; }

    .shortcut-text strong, .shortcut-text small { display: block; }
    .shortcut-text strong { color: var(--text-main); font-size: .82rem; font-weight: 700; transition: color .15s; }
    .shortcut-text small { margin-top: 2px; color: var(--text-muted); font-size: .7rem; }
    .arrow-icon { color: var(--text-muted); font-size: 20px; width: 20px; height: 20px; }

    .funnel-panel { margin-top: 18px; }
    .funnel-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
    .funnel-step { display: grid; gap: 8px; padding: 14px; border: 1px solid #e2e8f0; border-radius: 8px; }
    .funnel-step span { color: var(--text-secondary); font-size: .72rem; font-weight: 800; }
    .funnel-step strong { color: var(--text-main); font-size: 1.35rem; font-weight: 800; }
    .funnel-step small { color: var(--text-muted); font-size: .72rem; }
    .funnel-step .progress-track span { background: #0284c7; }

    .performance-panel, .campaign-panel, .export-panel { display: grid; gap: 16px; }
    .performance-heading { gap: 16px; margin-bottom: 0; }
    .table-controls { display: flex; align-items: center; }
    .table-controls input, .export-controls select {
      min-height: 38px; padding: 0 10px; border: 1px solid #cbd5e1; border-radius: 6px 0 0 6px;
      color: var(--text-main); background: #fff; font: inherit; font-size: .78rem;
    }
    .table-controls button, .table-pagination button {
      display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid #cbd5e1;
      color: #0369a1; background: #fff; cursor: pointer;
    }
    .table-controls button { border-left: 0; border-radius: 0 6px 6px 0; }
    .table-wrap { overflow-x: auto; border: 1px solid #e2e8f0; border-radius: 6px; }
    table { width: 100%; min-width: 820px; border-collapse: collapse; }
    th, td { padding: 11px 12px; border-bottom: 1px solid #e2e8f0; text-align: right; font-size: .76rem; }
    th:first-child, td:first-child { text-align: left; }
    th { color: #475569; background: #f8fafc; }
    th button { display: inline-flex; align-items: center; gap: 3px; border: 0; color: inherit; background: transparent; font: inherit; font-weight: 750; cursor: pointer; }
    th mat-icon { width: 15px; height: 15px; font-size: 15px; }
    td a { color: #0369a1; font-weight: 700; text-decoration: none; }
    .table-loading, .table-empty { padding: 28px; color: var(--text-muted); text-align: center; font-size: .78rem; }
    .table-error { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 16px; border: 1px solid #fecaca; border-radius: 6px; color: #b91c1c; background: #fef2f2; font-size: .78rem; }
    .table-error button { border: 0; color: #b91c1c; background: transparent; font: inherit; font-weight: 700; cursor: pointer; }
    .table-pagination { display: flex; align-items: center; justify-content: flex-end; gap: 10px; color: var(--text-muted); font-size: .74rem; }
    .table-pagination span:first-child { margin-right: auto; }
    .table-pagination button { border-radius: 5px; }
    .table-pagination button:disabled { opacity: .4; cursor: not-allowed; }

    .chart-description { margin: -10px 0 0; color: var(--text-muted); font-size: .74rem; }
    .trend-chart { display: flex; align-items: flex-end; gap: 6px; min-height: 190px; padding: 16px 12px 0; overflow-x: auto; border: 1px solid #e2e8f0; border-radius: 6px; background: #f8fafc; }
    .trend-day { display: grid; flex: 1 0 38px; gap: 5px; min-width: 38px; text-align: center; }
    .trend-bars { display: flex; align-items: flex-end; justify-content: center; gap: 3px; height: 140px; }
    .trend-bars i { display: block; width: 11px; min-height: 2px; border-radius: 3px 3px 0 0; }
    .trend-impressions, .legend-impressions { background: #0369a1; }.trend-clicks, .legend-clicks { background: #7c3aed; }
    .trend-day span { color: var(--text-muted); font-size: .62rem; }
    .trend-legend { display: flex; justify-content: flex-end; gap: 14px; color: var(--text-muted); font-size: .68rem; }
    .trend-legend span { display: inline-flex; align-items: center; gap: 5px; }.trend-legend i { width: 9px; height: 9px; border-radius: 2px; }
    .campaign-grid { display: grid; gap: 12px; }
    .campaign-row { display: grid; grid-template-columns: minmax(180px, .7fr) minmax(360px, 2fr) 90px; align-items: center; gap: 18px; padding: 16px; border: 1px solid #e2e8f0; border-radius: 6px; }
    .campaign-copy strong, .campaign-copy span { display: block; }
    .campaign-copy strong { font-size: .82rem; }
    .campaign-copy span { margin-top: 4px; color: var(--text-muted); font-size: .7rem; }
    .metric-bars { display: grid; gap: 7px; }
    .metric-bars > div { display: grid; grid-template-columns: 72px minmax(80px, 1fr) 34px; align-items: center; gap: 8px; }
    .metric-bars span, .metric-bars b { color: var(--text-muted); font-size: .68rem; font-style: normal; }
    .metric-bars b { text-align: right; }
    .bar { display: block; min-width: 2px; height: 7px; border-radius: 3px; }
    .impressions { background: #0369a1; } .clicks { background: #7c3aed; } .carts { background: #d97706; } .orders-bar { background: #059669; }
    .ctr { display: grid; gap: 3px; padding-left: 12px; border-left: 1px solid #e2e8f0; }
    .ctr span { color: var(--text-muted); font-size: .68rem; }.ctr strong { font-size: 1.2rem; }
    .export-panel { grid-template-columns: 1fr auto; align-items: center; }
    .export-panel h2 { margin: 4px 0 0; }
    .export-controls { display: flex; }
    .export-controls select { border-radius: 6px 0 0 6px; }
    .export-controls button, .export-status button { display: inline-flex; align-items: center; gap: 6px; min-height: 38px; padding: 0 14px; border: 0; border-radius: 0 6px 6px 0; color: #fff; background: #0369a1; font: inherit; font-size: .76rem; font-weight: 700; cursor: pointer; }
    .export-controls mat-icon { width: 17px; height: 17px; font-size: 17px; }
    .export-status { grid-column: 1 / -1; display: flex; align-items: center; gap: 12px; padding: 13px; border: 1px solid #bfdbfe; border-radius: 6px; background: #eff6ff; }
    .export-status div { flex: 1; }.export-status strong, .export-status span { display: block; }.export-status strong { font-size: .78rem; }.export-status span { color: var(--text-muted); font-size: .7rem; }
    .export-status button { border-radius: 6px; }
    .export-error { grid-column: 1 / -1; padding: 12px; border: 1px solid #fecaca; border-radius: 6px; color: #b91c1c; background: #fef2f2; font-size: .76rem; }

    .error-banner {
      display: flex; align-items: center; gap: 12px; padding: 16px 20px; border: 1px solid rgba(248, 113, 113, .3);
      color: #dc2626; background: #fef2f2;
    }
    .error-banner div { flex: 1; }
    .error-banner strong, .error-banner span { display: block; }
    .error-banner strong { font-size: .82rem; }
    .error-banner span { margin-top: 3px; color: var(--text-secondary); font-size: .72rem; }
    .error-banner button { border: 0; color: #dc2626; background: transparent; font-weight: 700; cursor: pointer; }
    
    .skeleton {
      height: 150px; border-radius: 16px; background: #e2e8f0;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    @keyframes shimmer { to { background-position: -200% 0; } }

    @media (max-width: 1050px) {
      .kpi-grid, .loading-grid { grid-template-columns: repeat(2, 1fr); }
      .analytics-kpi-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
      .analytics-loading { grid-template-columns: repeat(3, minmax(0, 1fr)); }
      .analytics-filters { grid-template-columns: repeat(3, minmax(130px, 1fr)); }
      .content-grid { grid-template-columns: 1fr; }
      .funnel-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .campaign-row { grid-template-columns: 1fr; }
      .ctr { padding: 0; border: 0; }
    }
    @media (max-width: 700px) {
      .page-header { align-items: flex-start; flex-direction: column; }
      .header-actions { width: 100%; justify-content: space-between; }
      .analytics-toolbar { align-items: stretch; flex-wrap: wrap; }
      .range-presets { width: 100%; }
      .range-presets button { flex: 1; }
      .analytics-toolbar label { flex: 1 1 130px; }
      .analytics-filters { grid-template-columns: 1fr 1fr; }
      .analytics-filters .clear-filters { grid-column: 1 / -1; }
      .analytics-kpi-grid { grid-template-columns: 1fr; }
      .analytics-loading { grid-template-columns: 1fr; }
      .section-heading { align-items: flex-start; flex-direction: column; }
      .funnel-grid { grid-template-columns: 1fr; }
      .performance-heading { align-items: stretch; flex-direction: column; }
      .table-controls input { width: 100%; }
      .export-panel { grid-template-columns: 1fr; }
      .export-controls { width: 100%; }.export-controls select { min-width: 0; flex: 1; }
      .kpi-grid, .loading-grid { grid-template-columns: 1fr; }
      .workload-row { grid-template-columns: 110px 1fr 45px; gap: 10px; }
    }
  `]
})
export class DashboardComponent implements OnInit {
  readonly skeletonItems = Array.from({ length: 6 });
  readonly analyticsSkeletonItems = Array.from({ length: 5 });
  readonly rangePresets = [7, 30, 90];
  stats: DashboardStats | null = null;
  analyticsOverview: AnalyticsOverview | null = null;
  funnel: FunnelSummary | null = null;
  analyticsLoading = false;
  analyticsError = '';
  loading = false;
  errorMessage = '';
  lastUpdated: Date | null = null;
  selectedRangeDays: number | null = 30;
  fromDate = '';
  toDate = '';
  filterCategoryId: number | null = null;
  filterProductId: number | null = null;
  filterCampaign = '';
  filterPlacement = '';
  filterDeviceType = '';
  productRows: ProductPerformance[] = [];
  promotionRows: PromotionPerformance[] = [];
  promotionTrend: PromotionTrendPoint[] = [];
  productsLoading = false;
  productsError = '';
  productSearch = '';
  productPage = 0;
  productSize = 8;
  productTotal = 0;
  productTotalPages = 0;
  productSortBy = 'productViews';
  productSortDirection: 'asc' | 'desc' = 'desc';
  exportType: AnalyticsExportType = 'OVERVIEW';
  exportJob: AnalyticsExportJob | null = null;
  exportBusy = false;
  exportError = '';

  constructor(
    public authService: AuthService,
    private dashboardService: DashboardService
  ) {}

  get averageOrderValue(): number {
    return this.stats && this.stats.totalOrders > 0
      ? this.stats.totalRevenue / this.stats.totalOrders
      : 0;
  }

  get pendingRate(): number {
    return this.percentageOfOrders(this.stats?.pendingOrders ?? 0);
  }

  get fulfillmentRate(): number {
    return this.percentageOfOrders(this.stats?.completedOrders ?? 0);
  }

  get isDateRangeValid(): boolean {
    return Boolean(this.fromDate && this.toDate && this.fromDate <= this.toDate);
  }

  get analyticsFilters(): AnalyticsFilters {
    return {
      ...(this.filterCategoryId && this.filterCategoryId > 0 ? { categoryId: this.filterCategoryId } : {}),
      ...(this.filterProductId && this.filterProductId > 0 ? { productId: this.filterProductId } : {}),
      ...(this.filterCampaign.trim() ? { campaign: this.filterCampaign.trim() } : {}),
      ...(this.filterPlacement ? { placement: this.filterPlacement } : {}),
      ...(this.filterDeviceType ? { deviceType: this.filterDeviceType } : {})
    };
  }

  get analyticsEmpty(): boolean {
    if (!this.analyticsOverview) {
      return false;
    }
    return this.analyticsOverview.productViews === 0
      && this.analyticsOverview.addToCarts === 0
      && this.analyticsOverview.beginCheckouts === 0
      && this.analyticsOverview.orders === 0;
  }

  get analyticsStale(): boolean {
    if (!this.analyticsOverview?.lastUpdatedAt) {
      return false;
    }
    const updatedAt = new Date(this.analyticsOverview.lastUpdatedAt).getTime();
    return Number.isFinite(updatedAt) && Date.now() - updatedAt > 10 * 60 * 1000;
  }

  ngOnInit(): void {
    this.updateDateInputs(30);
    this.loadStats();
  }

  setRange(days: number): void {
    this.selectedRangeDays = days;
    this.updateDateInputs(days);
    this.loadAnalytics();
  }

  applyCustomRange(): void {
    if (!this.isDateRangeValid) {
      return;
    }
    this.selectedRangeDays = null;
    this.loadAnalytics();
  }

  clearFilters(): void {
    this.filterCategoryId = null;
    this.filterProductId = null;
    this.filterCampaign = '';
    this.filterPlacement = '';
    this.filterDeviceType = '';
    this.loadAnalytics();
  }

  loadStats(): void {
    if (this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.dashboardService.getDashboardStats().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.data) {
          this.stats = response.data;
          this.lastUpdated = new Date();
          this.loadAnalytics();
          return;
        }
        this.errorMessage = response.message || 'The server returned an empty response.';
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Check the backend connection and try again.';
      }
    });
  }

  loadAnalytics(): void {
    const from = new Date(`${this.fromDate}T00:00:00`);
    const to = new Date(`${this.toDate}T23:59:59.999`);
    this.analyticsLoading = true;
    this.analyticsError = '';
    this.analyticsOverview = null;
    this.funnel = null;
    this.promotionRows = [];
    this.promotionTrend = [];

    forkJoin({
      overview: this.dashboardService.getAnalyticsOverview(from, to, this.analyticsFilters),
      funnel: this.dashboardService.getFunnelSummary(from, to, this.analyticsFilters),
      promotion: this.dashboardService.getPromotionPerformance(from, to, this.analyticsFilters),
      trend: this.dashboardService.getPromotionTrend(from, to, this.analyticsFilters)
    }).pipe(
      finalize(() => this.analyticsLoading = false)
    ).subscribe({
      next: ({ overview, funnel, promotion, trend }) => {
        if (!overview.success || !funnel.success || !promotion.success || !trend.success) {
          this.analyticsError = overview.message || funnel.message || promotion.message || 'Analytics data could not be loaded.';
          return;
        }
        this.analyticsOverview = overview.data;
        this.funnel = funnel.data;
        this.promotionRows = promotion.data;
        this.promotionTrend = trend.data;
        this.productPage = 0;
        this.loadProducts();
      },
      error: (error) => {
        this.analyticsError = error.error?.message || 'Check the backend connection and try again.';
      }
    });
  }

  loadProducts(): void {
    const from = new Date(`${this.fromDate}T00:00:00`);
    const to = new Date(`${this.toDate}T23:59:59.999`);
    this.productsLoading = true;
    this.productsError = '';
    this.dashboardService.getProductPerformance(
      from, to, this.productPage, this.productSize, this.productSearch,
      this.productSortBy, this.productSortDirection, this.analyticsFilters
    ).pipe(finalize(() => this.productsLoading = false)).subscribe({
      next: response => {
        if (!response.success) {
          this.productsError = response.message || 'Product performance could not be loaded.';
          this.resetProductRows();
          return;
        }
        this.productRows = response.data.content;
        this.productTotal = response.data.totalElements;
        this.productTotalPages = response.data.totalPages;
      },
      error: error => {
        this.productsError = error.error?.message || 'Product performance could not be loaded.';
        this.resetProductRows();
      }
    });
  }

  applyProductSearch(): void {
    this.productPage = 0;
    this.loadProducts();
  }

  sortProducts(field: string): void {
    if (this.productSortBy === field) {
      this.productSortDirection = this.productSortDirection === 'desc' ? 'asc' : 'desc';
    } else {
      this.productSortBy = field;
      this.productSortDirection = field === 'productName' ? 'asc' : 'desc';
    }
    this.productPage = 0;
    this.loadProducts();
  }

  sortIcon(field: string): string {
    return this.productSortBy !== field ? 'unfold_more'
      : this.productSortDirection === 'asc' ? 'arrow_upward' : 'arrow_downward';
  }

  changeProductPage(delta: number): void {
    const next = this.productPage + delta;
    if (next < 0 || next >= this.productTotalPages) return;
    this.productPage = next;
    this.loadProducts();
  }

  barWidth(value: number): number {
    const max = Math.max(1, ...this.promotionRows.map(row => row.impressions));
    return Math.max(value > 0 ? 2 : 0, (value / max) * 100);
  }

  trendHeight(value: number): number {
    const max = Math.max(1, ...this.promotionTrend.map(point => point.impressions));
    return Math.max(value > 0 ? 2 : 0, (value / max) * 100);
  }

  campaignAria(row: PromotionPerformance): string {
    return `${row.campaign || 'Organic'}: ${row.impressions} impressions, ${row.clicks} clicks, `
      + `${row.addToCarts} add to carts, ${row.attributedOrders} attributed orders`;
  }

  createExport(): void {
    const from = new Date(`${this.fromDate}T00:00:00`);
    const to = new Date(`${this.toDate}T23:59:59.999`);
    this.exportBusy = true;
    this.exportJob = null;
    this.exportError = '';
    this.dashboardService.createExport(this.exportType, from, to, this.analyticsFilters).subscribe({
      next: response => {
        this.exportBusy = false;
        if (!response.success) {
          this.exportError = response.message || 'The export could not be created.';
          return;
        }
        this.exportJob = response.data;
        this.pollExport(response.data.id);
      },
      error: error => {
        this.exportBusy = false;
        this.exportError = error.error?.message || 'The export could not be created.';
      }
    });
  }

  downloadExport(): void {
    if (!this.exportJob) return;
    this.exportError = '';
    this.dashboardService.downloadExport(this.exportJob.id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = this.exportJob?.fileName || 'analytics-export.csv';
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: error => this.exportError = error.error?.message || 'The export download failed.'
    });
  }

  private pollExport(id: string, attempt = 0): void {
    if (attempt >= 20) {
      this.exportError = 'The export is taking longer than expected. Refresh its status and try again.';
      return;
    }
    setTimeout(() => this.dashboardService.getExport(id).subscribe({
      next: response => {
        if (!response.success) {
          this.exportError = response.message || 'The export status could not be loaded.';
          return;
        }
        this.exportJob = response.data;
        if (response.data.status === 'PENDING' || response.data.status === 'PROCESSING') {
          this.pollExport(id, attempt + 1);
        }
      },
      error: error => this.exportError = error.error?.message || 'The export status could not be loaded.'
    }), 750);
  }

  private resetProductRows(): void {
    this.productRows = [];
    this.productTotal = 0;
    this.productTotalPages = 0;
  }

  private updateDateInputs(days: number): void {
    const to = new Date();
    const from = new Date(to);
    from.setDate(from.getDate() - (days - 1));
    this.fromDate = this.dateInputValue(from);
    this.toDate = this.dateInputValue(to);
  }

  private dateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private percentageOfOrders(count: number): number {
    if (!this.stats || this.stats.totalOrders === 0) {
      return 0;
    }
    return Math.min(100, (count / this.stats.totalOrders) * 100);
  }
}
