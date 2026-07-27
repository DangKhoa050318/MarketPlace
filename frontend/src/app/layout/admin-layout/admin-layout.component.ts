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
        <a class="portal-brand" routerLink="/admin/dashboard">
          <span class="brand-mark"><mat-icon>local_shipping</mat-icon></span>
          <span class="brand-copy">
            <strong>Order<span>Flow</span></strong>
            <small>Admin Portal</small>
          </span>
        </a>

        <div class="portal-badge">
          <mat-icon>shield</mat-icon>
          <div>
            <strong>Administration</strong>
            <small>Protected Control Center</small>
          </div>
        </div>

        <nav class="portal-nav" aria-label="Admin navigation">
          <span class="nav-heading">Overview</span>
          <a routerLink="/admin/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
            <mat-icon>space_dashboard</mat-icon>
            <span>Dashboard</span>
          </a>

          <span class="nav-heading">Inventory & Orders</span>
          <a routerLink="/admin/products" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
            <mat-icon>inventory_2</mat-icon>
            <span>Products Management</span>
          </a>
          <a routerLink="/admin/orders" routerLinkActive="active">
            <mat-icon>receipt_long</mat-icon>
            <span>Order Processing</span>
          </a>

          <span class="nav-heading">Administration</span>
          <a routerLink="/admin/users" routerLinkActive="active">
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
            <span>OrderFlow Portal</span>
            <mat-icon>chevron_right</mat-icon>
            <strong>Admin Dashboard</strong>
          </div>

          <span class="spacer"></span>

          <div class="system-health">
            <span class="health-dot"></span>
            System operational
          </div>

          <div class="admin-profile">
            <span class="avatar">{{ (authService.getUsername() || 'A')[0].toUpperCase() }}</span>
            <span class="profile-copy">
              <strong>{{ authService.getUsername() }}</strong>
              <small>Administrator</small>
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
      border-right: 1px solid var(--border-subtle);
      background: #ffffff;
      box-shadow: var(--shadow-sm);
    }

    :host ::ng-deep .admin-sidebar .mat-drawer-inner-container {
      display: flex;
      flex-direction: column;
      overflow-x: hidden;
    }

    .portal-brand {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0 18px;
      padding: 22px 4px 18px;
      color: var(--text-main);
      text-decoration: none;
    }

    .brand-mark {
      display: grid;
      width: 40px;
      height: 40px;
      flex: 0 0 40px;
      place-items: center;
      border-radius: 10px;
      color: #fff;
      background: #4f46e5;
      box-shadow: 0 4px 10px rgba(79, 70, 229, 0.25);
    }

    .brand-mark mat-icon { width: 22px; height: 22px; font-size: 22px; }
    .brand-copy strong, .brand-copy small { display: block; }
    .brand-copy strong { font-size: 1.1rem; letter-spacing: -.035em; color: var(--text-main); }
    .brand-copy strong span { color: #0284c7; }
    .brand-copy small {
      margin-top: 2px; color: var(--text-muted); font-size: .62rem;
      font-weight: 750; letter-spacing: .1em; text-transform: uppercase;
    }

    .portal-badge {
      display: flex;
      align-items: center;
      gap: 10px;
      margin: 0 16px 20px;
      padding: 10px 12px;
      border: 1px solid var(--border-subtle);
      border-radius: 10px;
      color: #4f46e5;
      background: #eef2ff;
    }
    .portal-badge mat-icon { width: 20px; height: 20px; font-size: 20px; color: #4f46e5; }
    .portal-badge strong, .portal-badge small { display: block; }
    .portal-badge strong { font-size: .72rem; color: #3730a3; }
    .portal-badge small { margin-top: 2px; color: #6366f1; font-size: .6rem; }

    .portal-nav { display: flex; flex-direction: column; padding: 0 12px; }
    .nav-heading {
      margin: 16px 12px 6px;
      color: var(--text-muted);
      font-size: .62rem;
      font-weight: 800;
      letter-spacing: .13em;
      text-transform: uppercase;
    }
    .portal-nav a {
      display: flex;
      align-items: center;
      gap: 11px;
      min-height: 42px;
      padding: 0 12px;
      border-radius: 10px;
      color: var(--text-secondary);
      font-size: .82rem;
      font-weight: 650;
      text-decoration: none;
      transition: color .15s ease, background-color .15s ease;
    }
    .portal-nav a:hover { color: #4f46e5; background: #f1f5f9; }
    .portal-nav a.active { color: #4338ca; background: #eef2ff; font-weight: 800; }
    .portal-nav a mat-icon { width: 20px; height: 20px; font-size: 20px; }

    .sidebar-footer {
      margin-top: auto;
      padding: 16px;
      border-top: 1px solid var(--border-subtle);
    }
    .sidebar-footer a {
      display: grid;
      grid-template-columns: 28px 1fr 16px;
      align-items: center;
      gap: 8px;
      padding: 10px 12px;
      border: 1px solid var(--border-subtle);
      border-radius: 10px;
      color: var(--text-secondary);
      background: #f8fafc;
      text-decoration: none;
      transition: all 0.2s ease;
    }
    .sidebar-footer a:hover {
      border-color: var(--primary);
      background: #ffffff;
    }
    .sidebar-footer > a > mat-icon:first-child { color: #0284c7; }
    .sidebar-footer strong, .sidebar-footer small { display: block; }
    .sidebar-footer strong { font-size: .7rem; color: var(--text-main); }
    .sidebar-footer small { margin-top: 2px; color: var(--text-muted); font-size: .6rem; }
    .open-icon { color: var(--text-muted); width: 16px; height: 16px; font-size: 16px; }

    .portal-content {
      background: var(--bg-main);
    }
    .portal-topbar {
      position: sticky;
      top: 0;
      z-index: 20;
      height: 65px;
      padding: 0 28px;
      border-bottom: 1px solid var(--border-subtle);
      background: #ffffff !important;
      color: var(--text-main);
      box-shadow: var(--shadow-sm);
    }
    .breadcrumb { display: flex; align-items: center; gap: 6px; font-size: .78rem; }
    .breadcrumb span { color: var(--text-muted); }
    .breadcrumb strong { color: var(--text-main); font-weight: 700; }
    .breadcrumb mat-icon { color: #cbd5e1; width: 16px; height: 16px; font-size: 16px; }
    .spacer { flex: 1; }
    .system-health {
      display: flex;
      align-items: center;
      gap: 7px;
      margin-right: 20px;
      color: var(--text-secondary);
      font-size: .72rem;
      font-weight: 650;
    }
    .health-dot {
      width: 8px; height: 8px; border-radius: 50%;
      background: #10b981;
    }
    .admin-profile { display: flex; align-items: center; gap: 10px; margin-right: 12px; }
    .avatar {
      display: grid; width: 34px; height: 34px; place-items: center; border-radius: 8px;
      color: #fff; background: #4f46e5;
      font-size: .8rem; font-weight: 800;
    }
    .profile-copy strong, .profile-copy small { display: block; line-height: 1.25; }
    .profile-copy strong { color: var(--text-main); font-size: .76rem; }
    .profile-copy small { color: var(--text-muted); font-size: .6rem; }
    .logout-button { color: var(--text-muted); }

    .portal-main {
      box-sizing: border-box;
      width: 100%;
      max-width: 1500px;
      min-height: calc(100dvh - 65px);
      margin: 0 auto;
      padding: 28px 30px 40px;
    }

    @media (max-width: 900px) {
      .admin-sidebar { width: 220px; }
      .system-health, .profile-copy { display: none; }
      .portal-main { padding: 23px 20px 36px; }
    }

    @media (max-width: 680px) {
      .admin-sidebar { width: 76px; }
      .portal-brand { justify-content: center; margin: 0; }
      .brand-copy, .portal-badge, .nav-heading, .portal-nav a span,
      .sidebar-footer span, .sidebar-footer .open-icon { display: none; }
      .portal-nav a { justify-content: center; padding: 0; }
      .sidebar-footer a { display: flex; justify-content: center; padding: 10px 0; }
      .breadcrumb { display: none; }
      .portal-topbar { padding: 0 14px; }
      .portal-main { padding: 20px 14px 32px; }
    }
  `]
})
export class AdminLayoutComponent {
  constructor(public authService: AuthService) {}
}
