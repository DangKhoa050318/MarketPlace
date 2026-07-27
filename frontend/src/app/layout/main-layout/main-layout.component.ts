import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';

import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { ThreeBgComponent } from '../../shared/components/three-bg/three-bg.component';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatMenuModule,
    MatDividerModule,
    ThreeBgComponent
  ],
  template: `
    <div class="user-layout-wrapper">
      <!-- Floating Solid Navbar -->
      <header class="solid-navbar">
        <div class="nav-container">
          <!-- Brand Logo -->
          <a routerLink="/products" class="brand-logo">
            <div class="logo-icon"><mat-icon>layers</mat-icon></div>
            <div class="logo-brand-wrap">
              <span class="logo-text">OrderFlow</span>
              <span class="logo-subtext">Engine</span>
            </div>
          </a>

          <!-- Nav Links -->
          <nav class="nav-links">
            <a routerLink="/products" routerLinkActive="active-link" class="nav-item">
              <mat-icon>storefront</mat-icon>
              <span>Storefront</span>
            </a>
            <a routerLink="/orders" routerLinkActive="active-link" class="nav-item">
              <mat-icon>receipt_long</mat-icon>
              <span>My Orders</span>
            </a>

            <!-- Single Dedicated Admin Dashboard Button (Only visible if logged-in user is ADMIN) -->
            <a *ngIf="authService.getRole() === 'ADMIN'" routerLink="/admin/dashboard" class="nav-item admin-badge-link" title="Open Admin Portal">
              <mat-icon>space_dashboard</mat-icon>
              <span>Admin Dashboard</span>
            </a>
          </nav>

          <!-- User Profile & Actions -->
          <div class="user-controls">
            <a mat-icon-button routerLink="/cart" class="cart-btn" title="View Cart">
              <mat-icon [matBadge]="cartCount$ | async" [matBadgeHidden]="(cartCount$ | async) === 0" matBadgeColor="warn">shopping_bag</mat-icon>
            </a>

            <div class="user-profile-menu" [matMenuTriggerFor]="userMenu">
              <div class="avatar-ring">
                <span class="avatar-initial">{{ (authService.getUsername() || 'U')[0].toUpperCase() }}</span>
              </div>
              <div class="user-info-brief">
                <span class="user-name">{{ authService.getUsername() }}</span>
              </div>
              <mat-icon class="dropdown-icon">expand_more</mat-icon>
            </div>

            <mat-menu #userMenu="matMenu" class="glass-menu">
              <div class="menu-header">
                <div class="menu-user-title">{{ authService.getUsername() }}</div>
              </div>
              <button mat-menu-item routerLink="/products">
                <mat-icon>storefront</mat-icon>
                <span>Browse Products</span>
              </button>
              <button mat-menu-item routerLink="/cart">
                <mat-icon>shopping_cart</mat-icon>
                <span>My Cart</span>
              </button>
              <button mat-menu-item routerLink="/orders">
                <mat-icon>receipt_long</mat-icon>
                <span>My Orders</span>
              </button>
              <button *ngIf="authService.getRole() === 'ADMIN'" mat-menu-item routerLink="/admin/dashboard">
                <mat-icon color="primary">admin_panel_settings</mat-icon>
                <span>Admin Dashboard</span>
              </button>
              <mat-divider></mat-divider>
              <button mat-menu-item (click)="authService.logout()" class="logout-menu-item">
                <mat-icon color="warn">logout</mat-icon>
                <span style="color: #ef4444;">Sign Out</span>
              </button>
            </mat-menu>
          </div>
        </div>
      </header>

      <!-- Main Page Content -->
      <main class="page-content">
        <router-outlet></router-outlet>
      </main>

      <!-- Clean Enterprise Footer -->
      <footer class="solid-footer">
        <div class="footer-content">
          <div class="footer-brand">
            <span>&copy; 2026 OrderFlow Platform Inc. All rights reserved.</span>
          </div>
          <div class="footer-links">
            <a routerLink="/products">Catalog</a>
            <span>•</span>
            <a routerLink="/cart">Cart</a>
            <span>•</span>
            <a routerLink="/orders">My Orders</a>
            <ng-container *ngIf="authService.getRole() === 'ADMIN'">
              <span>•</span>
              <a routerLink="/admin/dashboard">Admin Portal</a>
            </ng-container>
          </div>
        </div>
      </footer>
    </div>
  `,
  styles: [`
    .user-layout-wrapper {
      position: relative;
      z-index: 1;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      background-color: var(--bg-main);
    }

    /* Solid Navbar */
    .solid-navbar {
      position: sticky;
      top: 0;
      z-index: 100;
      background: #ffffff;
      border-bottom: 1px solid var(--border-subtle);
      box-shadow: var(--shadow-sm);
      padding: 0 24px;
    }

    .nav-container {
      max-width: 1280px;
      height: 70px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .brand-logo {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
    }

    .logo-icon {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      background: var(--primary);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      box-shadow: 0 4px 10px rgba(79, 70, 229, 0.25);
    }

    .logo-brand-wrap {
      display: flex;
      flex-direction: column;
      line-height: 1.1;
    }

    .logo-text {
      font-size: 1.25rem;
      font-weight: 800;
      letter-spacing: -0.5px;
      color: #0f172a;
    }

    .logo-subtext {
      font-size: 0.68rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: var(--primary);
    }

    .nav-links {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 14px;
      border-radius: 10px;
      color: var(--text-secondary);
      text-decoration: none;
      font-weight: 650;
      font-size: 0.9rem;
      transition: all 0.15s ease;
    }

    .nav-item:hover {
      color: var(--primary);
      background: var(--primary-subtle);
    }

    .active-link {
      color: var(--primary) !important;
      background: var(--primary-subtle) !important;
      font-weight: 700;
    }

    .admin-badge-link {
      color: #7c3aed !important;
      background: #f3e8ff;
      border: 1px solid rgba(124, 58, 237, 0.2);
    }

    .admin-badge-link:hover {
      background: #e9d5ff !important;
    }

    .user-controls {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .cart-btn {
      color: #334155;
    }

    .user-profile-menu {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 5px 12px;
      border-radius: 10px;
      background: #ffffff;
      border: 1px solid var(--border-subtle);
      box-shadow: var(--shadow-sm);
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .user-profile-menu:hover {
      border-color: var(--border-strong);
    }

    .avatar-ring {
      width: 32px;
      height: 32px;
      border-radius: 8px;
      background: var(--primary);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      color: #fff;
      font-size: 0.85rem;
    }

    .user-info-brief {
      display: flex;
      flex-direction: column;
      line-height: 1.2;
    }

    .user-name {
      font-weight: 700;
      font-size: 0.85rem;
      color: #0f172a;
    }

    .dropdown-icon {
      color: var(--text-muted);
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .menu-header {
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-subtle);
    }

    .menu-user-title {
      font-weight: 700;
      color: #0f172a;
    }

    .page-content {
      flex: 1;
      max-width: 1280px;
      width: 100%;
      margin: 0 auto;
      padding: 32px 24px;
      box-sizing: border-box;
    }

    .solid-footer {
      background: #ffffff;
      border-top: 1px solid var(--border-subtle);
      padding: 20px 24px;
      margin-top: 40px;
    }

    .footer-content {
      max-width: 1280px;
      margin: 0 auto;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
      color: var(--text-secondary);
      font-size: 0.85rem;
    }

    .footer-links {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .footer-links a {
      color: var(--text-secondary);
      text-decoration: none;
      font-weight: 600;
      transition: color 0.15s ease;
    }

    .footer-links a:hover {
      color: var(--primary);
    }
  `]
})
export class MainLayoutComponent implements OnInit {
  cartCount$: Observable<number>;

  constructor(
    public authService: AuthService,
    private cartService: CartService
  ) {
    this.cartCount$ = this.cartService.cartCount$;
  }

  ngOnInit(): void {
    this.cartService.getCart().subscribe({
      error: () => {}
    });
  }
}
