import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService, DashboardStats } from '../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, MatButtonModule],
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
        <div class="kpi-grid">
          <article class="kpi-card surface-card surface-card-hover">
            <div class="kpi-top">
              <span class="icon revenue"><mat-icon>payments</mat-icon></span>
              <span class="context">Confirmed revenue</span>
            </div>
            <span class="label">Total Gross Revenue</span>
            <strong class="kpi-amount revenue-text">{{ data.totalRevenue | currency:'USD':'symbol':'1.2-2' }}</strong>
            <small>Recognized across {{ data.totalOrders | number }} orders</small>
          </article>

          <article class="kpi-card surface-card surface-card-hover clickable-card" [routerLink]="['/admin/orders']">
            <div class="kpi-top">
              <span class="icon orders"><mat-icon>receipt_long</mat-icon></span>
              <span class="context">{{ pendingRate | number:'1.0-1' }}% pending</span>
            </div>
            <span class="label">Total Orders Processed</span>
            <strong class="kpi-amount">{{ data.totalOrders | number }}</strong>
            <small>{{ data.pendingOrders | number }} orders require fulfillment</small>
          </article>

          <article class="kpi-card surface-card surface-card-hover">
            <div class="kpi-top">
              <span class="icon average"><mat-icon>analytics</mat-icon></span>
              <span class="context">Average per transaction</span>
            </div>
            <span class="label">Average Order Value</span>
            <strong class="kpi-amount">{{ averageOrderValue | currency:'USD':'symbol':'1.2-2' }}</strong>
            <small>Calculated from customer checkout data</small>
          </article>

          <article class="kpi-card surface-card surface-card-hover clickable-card" [routerLink]="['/admin/orders']" [queryParams]="{ status: 'DELIVERED' }">
            <div class="kpi-top">
              <span class="icon delivery"><mat-icon>task_alt</mat-icon></span>
              <span class="context">{{ fulfillmentRate | number:'1.0-1' }}% fulfillment</span>
            </div>
            <span class="label">Delivered Orders</span>
            <strong class="kpi-amount">{{ data.completedOrders | number }}</strong>
            <small>Completed full delivery lifecycle</small>
          </article>

          <article class="kpi-card surface-card surface-card-hover clickable-card" routerLink="/admin/products">
            <div class="kpi-top">
              <span class="icon products"><mat-icon>inventory_2</mat-icon></span>
              <span class="context">Catalog items</span>
            </div>
            <span class="label">Catalog Products</span>
            <strong class="kpi-amount">{{ data.totalProducts | number }}</strong>
            <small>Active products listed on storefront</small>
          </article>

          <article class="kpi-card surface-card surface-card-hover clickable-card" routerLink="/admin/users">
            <div class="kpi-top">
              <span class="icon customers"><mat-icon>group</mat-icon></span>
              <span class="context">Registered accounts</span>
            </div>
            <span class="label">Registered Customers</span>
            <strong class="kpi-amount">{{ data.totalCustomers | number }}</strong>
            <small>Active user accounts in system</small>
          </article>
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

            <div class="workload-row clickable-row" [routerLink]="['/admin/orders']" [queryParams]="{ status: 'PENDING' }">
              <div class="workload-copy">
                <span>Pending Queue</span>
                <strong>{{ data.pendingOrders | number }}</strong>
              </div>
              <div class="progress-track" aria-label="Pending order percentage">
                <span class="pending-progress" [style.width.%]="pendingRate"></span>
              </div>
              <span class="percentage">{{ pendingRate | number:'1.0-1' }}%</span>
            </div>

            <div class="workload-row clickable-row" [routerLink]="['/admin/orders']" [queryParams]="{ status: 'DELIVERED' }">
              <div class="workload-copy">
                <span>Delivered</span>
                <strong>{{ data.completedOrders | number }}</strong>
              </div>
              <div class="progress-track" aria-label="Delivered order percentage">
                <span class="delivered-progress" [style.width.%]="fulfillmentRate"></span>
              </div>
              <span class="percentage">{{ fulfillmentRate | number:'1.0-1' }}%</span>
            </div>

            <div class="attention clickable-attention" [class.clear]="data.pendingOrders === 0" [routerLink]="['/admin/orders']" [queryParams]="{ status: 'PENDING' }">
              <mat-icon>{{ data.pendingOrders > 0 ? 'notification_important' : 'check_circle' }}</mat-icon>
              <div>
                <strong>{{ data.pendingOrders > 0 ? 'Action Needed: Pending Orders' : 'Fulfillment Status Operational' }}</strong>
                <span *ngIf="data.pendingOrders > 0">
                  {{ data.pendingOrders }} order{{ data.pendingOrders === 1 ? '' : 's' }} awaiting admin status update. Click to view pending queue.
                </span>
                <span *ngIf="data.pendingOrders === 0">No orders currently pending review.</span>
              </div>
            </div>
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

    @media (max-width: 1050px) {
      .kpi-grid, .loading-grid { grid-template-columns: repeat(2, 1fr); }
      .content-grid { grid-template-columns: 1fr; }
    }
    @media (max-width: 700px) {
      .page-header { align-items: flex-start; flex-direction: column; }
      .header-actions { width: 100%; justify-content: space-between; }
      .kpi-grid, .loading-grid { grid-template-columns: 1fr; }
      .workload-row { grid-template-columns: 110px 1fr 45px; gap: 10px; }
    }
  `]
})
export class DashboardComponent implements OnInit {
  readonly skeletonItems = Array.from({ length: 6 });
  stats: DashboardStats | null = null;
  loading = false;
  errorMessage = '';
  lastUpdated: Date | null = null;

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

  ngOnInit(): void {
    this.loadStats();
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

  private percentageOfOrders(count: number): number {
    if (!this.stats || this.stats.totalOrders === 0) {
      return 0;
    }
    return Math.min(100, (count / this.stats.totalOrders) * 100);
  }
}
