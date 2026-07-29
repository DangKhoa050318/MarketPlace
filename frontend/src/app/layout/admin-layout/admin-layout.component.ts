import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule
  ],
  template: `
    <mat-sidenav-container class="admin-shell">
      <mat-sidenav mode="side" opened class="admin-sidebar">
        <a class="portal-brand" [routerLink]="hasRole(['ADMIN', 'MANAGER']) ? '/admin/dashboard' : '/stock'">
          <span class="brand-mark"><mat-icon>local_shipping</mat-icon></span>
          <span class="brand-copy">
            <strong>Market<span>Place</span></strong>
            <small>Management Portal</small>
          </span>
        </a>

        <div class="portal-badge">
          <mat-icon>shield</mat-icon>
          <div>
            <strong>{{ authService.getRole() }}</strong>
            <small>Back-Office Center</small>
          </div>
        </div>

        <nav class="portal-nav" aria-label="Admin navigation">
          <span *ngIf="hasRole(['ADMIN', 'MANAGER'])" class="nav-heading">Overview</span>
          <a *ngIf="hasRole(['ADMIN', 'MANAGER'])" routerLink="/admin/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
            <mat-icon>space_dashboard</mat-icon>
            <span>Dashboard</span>
          </a>

          <span class="nav-heading">Inventory & Orders</span>
          <a *ngIf="hasRole(['ADMIN', 'MANAGER'])" routerLink="/admin/products" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
            <mat-icon>inventory_2</mat-icon>
            <span>Products Management</span>
          </a>
          <a *ngIf="hasRole(['ADMIN', 'MANAGER', 'STAFF'])" routerLink="/admin/orders" routerLinkActive="active">
            <mat-icon>receipt_long</mat-icon>
            <span>Order Processing</span>
          </a>
          <a *ngIf="hasRole(['ADMIN', 'MANAGER', 'STAFF'])" routerLink="/admin/moderation" routerLinkActive="active">
            <mat-icon>verified_user</mat-icon>
            <span>Content Moderation</span>
          </a>
          <a *ngIf="hasRole(['ADMIN'])" routerLink="/admin/coupons" routerLinkActive="active">
            <mat-icon>local_offer</mat-icon>
            <span>Coupons</span>
          </a>

          <span class="nav-heading">Warehouse & Stock</span>
          <a *ngIf="hasRole(['ADMIN', 'MANAGER', 'STAFF'])" routerLink="/warehouses" routerLinkActive="active">
            <mat-icon>warehouse</mat-icon>
            <span>Warehouses</span>
          </a>
          <a *ngIf="hasRole(['ADMIN', 'MANAGER', 'STAFF'])" routerLink="/stock" routerLinkActive="active">
            <mat-icon>inventory</mat-icon>
            <span>Stock Levels</span>
          </a>
          <a *ngIf="hasRole(['ADMIN', 'MANAGER', 'STAFF'])" routerLink="/stock/movements" routerLinkActive="active">
            <mat-icon>swap_horiz</mat-icon>
            <span>Stock Movements</span>
          </a>

          <span *ngIf="hasRole(['ADMIN'])" class="nav-heading">Administration</span>
          <a *ngIf="hasRole(['ADMIN'])" routerLink="/admin/users" routerLinkActive="active">
            <mat-icon>manage_accounts</mat-icon>
            <span>User Accounts</span>
          </a>
        </nav>

        <div class="sidebar-footer">
          <a routerLink="/products">
            <mat-icon>storefront</mat-icon>
            <span>
              <strong>Customer Storefront</strong>
              <small>Switch to catalog view</small>
            </span>
            <mat-icon class="open-icon">north_east</mat-icon>
          </a>
        </div>
      </mat-sidenav>

      <mat-sidenav-content class="portal-content">
        <mat-toolbar class="portal-topbar">
          <div class="breadcrumb">
            <span>Marketplace Portal</span>
            <mat-icon>chevron_right</mat-icon>
            <strong>{{ authService.getRole() }} View</strong>
          </div>

          <span class="spacer"></span>

          <div class="system-health">
            <span class="health-dot"></span>
            System operational
          </div>

          <div class="admin-profile">
            <span class="avatar">{{ (authService.getUsername() || 'U')[0].toUpperCase() }}</span>
            <span class="profile-copy">
              <strong>{{ authService.getUsername() }}</strong>
              <small>{{ authService.getRole() }}</small>
            </span>
          </div>

          <button mat-icon-button (click)="authService.logout()" class="logout-button" title="Sign out">
            <mat-icon>logout</mat-icon>
          </button>
        </mat-toolbar>

        <main class="portal-main">
          <router-outlet></router-outlet>
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    :host { display: block; }

    .admin-shell {
      height: 100dvh;
      color: var(--text-main);
      background: var(--bg-main);
    }

    .admin-sidebar {
      width: 260px;
      background: #ffffff !important;
      border-right: 1px solid var(--border-subtle) !important;
      display: flex;
      flex-direction: column;
      padding: 24px 16px;
      box-sizing: border-box;
      box-shadow: var(--shadow-sm);
    }

    .portal-brand {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
      color: var(--text-main);
      margin-bottom: 20px;
      padding: 0 8px;
    }

    .brand-mark {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      background: var(--primary);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    .brand-copy strong { font-size: 1.2rem; display: block; color: var(--text-main); }
    .brand-copy strong span { color: var(--primary); }
    .brand-copy small { color: var(--text-muted); font-size: 0.75rem; }

    .portal-badge {
      display: flex;
      align-items: center;
      gap: 10px;
      background: var(--primary-subtle);
      border: 1px solid var(--border-subtle);
      border-radius: 10px;
      padding: 10px 12px;
      color: var(--primary);
      margin-bottom: 20px;
    }

    .portal-badge strong { display: block; font-size: 0.85rem; color: var(--primary-text); }
    .portal-badge small { color: var(--text-secondary); font-size: 0.7rem; }

    .portal-nav {
      display: flex;
      flex-direction: column;
      gap: 4px;
      flex: 1;
    }

    .nav-heading {
      font-size: 0.7rem;
      font-weight: 700;
      text-transform: uppercase;
      color: var(--text-muted);
      letter-spacing: 0.05em;
      margin: 16px 8px 6px;
    }

    .portal-nav a {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      border-radius: 8px;
      color: var(--text-secondary);
      text-decoration: none;
      font-weight: 500;
      transition: all 0.15s ease;
    }

    .portal-nav a:hover {
      background: var(--surface-hover);
      color: var(--text-main);
    }

    .portal-nav a.active {
      background: var(--primary-subtle);
      color: var(--primary);
      font-weight: 700;
      border: 1px solid var(--border-primary);
    }

    .sidebar-footer {
      margin-top: auto;
      padding-top: 16px;
      border-top: 1px solid var(--border-subtle);
    }

    .sidebar-footer a {
      display: flex;
      align-items: center;
      gap: 10px;
      color: var(--text-secondary);
      text-decoration: none;
      padding: 8px;
      border-radius: 8px;
    }

    .sidebar-footer a:hover {
      color: var(--primary);
      background: var(--surface-hover);
    }

    .sidebar-footer strong { display: block; font-size: 0.85rem; color: var(--text-main); }
    .sidebar-footer small { color: var(--text-muted); font-size: 0.7rem; }

    .portal-content {
      background: var(--bg-main);
      display: flex;
      flex-direction: column;
    }

    .portal-topbar {
      background: #ffffff !important;
      border-bottom: 1px solid var(--border-subtle);
      color: var(--text-main);
      padding: 0 24px;
      height: 64px;
      box-shadow: var(--shadow-sm);
    }

    .breadcrumb {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.9rem;
      color: var(--text-secondary);
    }

    .breadcrumb strong { color: var(--text-main); }

    .spacer { flex: 1; }

    .system-health {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.8rem;
      color: var(--success-text);
      background: var(--success-subtle);
      padding: 4px 12px;
      border-radius: 9999px;
      border: 1px solid var(--success);
      margin-right: 16px;
    }

    .health-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--success);
    }

    .admin-profile {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-right: 12px;
    }

    .avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: var(--primary);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
    }

    .profile-copy { display: flex; flex-direction: column; justify-content: center; }
    .profile-copy strong { display: block; font-size: 0.85rem; color: var(--text-main); line-height: 1.2; }
    .profile-copy small { display: block; color: var(--text-muted); font-size: 0.7rem; text-transform: uppercase; line-height: 1.2; margin-top: 2px; }

    .logout-button { color: var(--text-secondary); }
    .logout-button:hover { color: var(--danger); }

    .portal-main {
      padding: 24px 32px;
      flex: 1;
    }
  `]
})
export class AdminLayoutComponent {
  constructor(public authService: AuthService) {}

  hasRole(roles: string[]): boolean {
    const role = this.authService.getRole() || '';
    return roles.includes(role);
  }
}
