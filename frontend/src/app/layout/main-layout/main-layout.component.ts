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
import { ChatAssistantComponent } from '../../shared/components/chat-assistant/chat-assistant.component';
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
    ThreeBgComponent,
    ChatAssistantComponent
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
              <span class="logo-text">MarketPlace</span>
              <span class="logo-subtext">Platform</span>
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
            <a *ngIf="authService.isAuthenticated()" routerLink="/wishlist" routerLinkActive="active-link" class="nav-item">
              <mat-icon>favorite</mat-icon>
              <span>Wishlist</span>
            </a>

            <!-- Single Dedicated Admin Dashboard Button (Only visible if logged-in user is ADMIN) -->
            <a *ngIf="authService.getRole() === 'ADMIN'" routerLink="/admin/dashboard" class="nav-item admin-badge-link" title="Open Admin Portal">
              <mat-icon>space_dashboard</mat-icon>
              <span>Admin Dashboard</span>
            </a>
          </nav>

            <!-- User Profile & Actions -->
            <div class="user-controls">
              @if (authService.isAuthenticated()) {
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
              } @else {
                <a mat-stroked-button routerLink="/login">Đăng nhập</a>
                <a mat-flat-button routerLink="/register" class="signup-button">Tạo tài khoản</a>
              }

              <mat-menu #userMenu="matMenu" class="user-dropdown-panel" xPosition="before">
                <div class="menu-header">
                  <div class="menu-user-info">
                    <div class="avatar-ring header-avatar">
                      <span class="avatar-initial">{{ (authService.getUsername() || 'U')[0].toUpperCase() }}</span>
                    </div>
                    <div class="menu-user-details">
                      <span class="menu-user-title">{{ authService.getUsername() }}</span>
                      <span class="menu-user-role">{{ authService.getRole() || 'CUSTOMER' }}</span>
                    </div>
                  </div>
                </div>
                <button mat-menu-item routerLink="/products">
                  <mat-icon>storefront</mat-icon>
                  <span>Browse Products</span>
                </button>
                <button mat-menu-item routerLink="/cart">
                  <mat-icon>shopping_cart</mat-icon>
                  <span>My Cart</span>
                </button>
                <button mat-menu-item routerLink="/wallet">
                  <mat-icon>account_balance_wallet</mat-icon>
                  <span>Marketplace Wallet</span>
                </button>
                <button mat-menu-item routerLink="/wishlist">
                  <mat-icon>favorite</mat-icon>
                  <span>Wishlist</span>
                </button>
                <button mat-menu-item routerLink="/orders">
                  <mat-icon>receipt_long</mat-icon>
                  <span>My Orders</span>
                </button>
                <button *ngIf="authService.getRole() === 'ADMIN'" mat-menu-item routerLink="/admin/dashboard">
                  <mat-icon style="color: #7c3aed;">space_dashboard</mat-icon>
                  <span>Admin Dashboard</span>
                </button>
                <mat-divider style="margin: 4px 0;"></mat-divider>
                <button mat-menu-item (click)="authService.logout()" class="logout-menu-item">
                  <mat-icon style="color: #ef4444;">logout</mat-icon>
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

      <app-chat-assistant></app-chat-assistant>

      <!-- Professional Footer -->
      <footer class="main-footer">
        <div class="footer-inner">
          <!-- Top row: 3 columns -->
          <div class="footer-grid">
            <!-- Brand -->
            <div class="footer-col brand-col">
              <a routerLink="/products" class="footer-logo-link">
                <div class="footer-logo-icon"><mat-icon>layers</mat-icon></div>
                <span class="footer-brand-name">MarketPlace</span>
              </a>
              <p class="footer-desc">
                A modern, single-seller marketplace platform built for scale.
                Discover premium products with confidence.
              </p>
              <div class="footer-social">
                <a href="#" class="social-link" title="Facebook" onclick="return false;">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                    <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                  </svg>
                </a>
                <a href="#" class="social-link" title="X (Twitter)" onclick="return false;">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                    <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/>
                  </svg>
                </a>
                <a href="#" class="social-link" title="Instagram" onclick="return false;">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                    <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z"/>
                  </svg>
                </a>
                <a href="#" class="social-link" title="LinkedIn" onclick="return false;">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                    <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433a2.062 2.062 0 01-2.063-2.065 2.064 2.064 0 112.063 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/>
                  </svg>
                </a>
              </div>
            </div>

            <!-- Quick Links -->
            <div class="footer-col">
              <h4 class="footer-heading">Quick Links</h4>
              <ul class="footer-ul">
                <li><a routerLink="/products">Catalog</a></li>
                <li><a routerLink="/cart">Shopping Cart</a></li>
                <li><a routerLink="/orders">My Orders</a></li>
                <li *ngIf="authService.getRole() === 'ADMIN'"><a routerLink="/admin/dashboard">Admin Dashboard</a></li>
              </ul>
            </div>

            <!-- Support -->
            <div class="footer-col">
              <h4 class="footer-heading">Support</h4>
              <ul class="footer-ul">
                <li><a href="#" onclick="return false;">Help Center</a></li>
                <li><a href="#" onclick="return false;">Shipping &amp; Returns</a></li>
                <li><a href="#" onclick="return false;">Privacy Policy</a></li>
                <li><a href="#" onclick="return false;">Terms of Service</a></li>
              </ul>
            </div>
          </div>

          <div class="footer-bottom">
            <span class="footer-copy">&copy; 2026 MarketPlace Inc. All rights reserved.</span>
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
      max-width: 1480px;
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
      gap: 8px;
      padding: 3px 10px 3px 4px;
      border-radius: 12px;
      background: #ffffff;
      border: 1px solid var(--border-subtle);
      box-shadow: var(--shadow-sm);
      cursor: pointer;
      height: 38px;
      box-sizing: border-box;
      transition: all 0.15s ease;
      user-select: none;
    }

    .user-profile-menu:hover {
      border-color: var(--border-strong);
      background: #f8fafc;
    }

    .avatar-ring {
      width: 30px;
      height: 30px;
      border-radius: 8px;
      background: linear-gradient(135deg, #4f46e5, #6366f1);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      color: #fff;
      font-size: 0.85rem;
      box-shadow: 0 2px 6px rgba(79, 70, 229, 0.25);
      flex-shrink: 0;
    }

    .user-info-brief {
      display: flex;
      align-items: center;
    }

    .user-name {
      font-weight: 650;
      font-size: 0.85rem;
      color: #0f172a;
      max-width: 120px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .dropdown-icon {
      color: var(--text-muted);
      font-size: 18px;
      width: 18px;
      height: 18px;
      transition: transform 0.15s ease, color 0.15s ease;
    }

    .user-profile-menu:hover .dropdown-icon {
      color: var(--text-main);
    }

    .page-content {
      flex: 1;
      max-width: 1480px;
      width: 100%;
      margin: 0 auto;
      padding: 24px 20px;
      box-sizing: border-box;
    }

    /* ── Professional Footer ── */
    .main-footer {
      background: #0f172a;
      color: #cbd5e1;
      margin-top: 64px;
      padding: 0;
    }

    .footer-inner {
      max-width: 1480px;
      margin: 0 auto;
      padding: 48px 24px 0;
    }

    .footer-grid {
      display: grid;
      grid-template-columns: 1.8fr 1fr 1fr;
      gap: 48px;
      padding-bottom: 40px;
      border-bottom: 1px solid rgba(255,255,255,0.08);
    }

    .footer-col {
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .footer-logo-link {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      text-decoration: none;
      color: #fff;
    }

    .footer-logo-icon {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      background: linear-gradient(135deg, #6366f1, #4f46e5);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
    }

    .footer-logo-icon mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .footer-brand-name {
      font-size: 1.15rem;
      font-weight: 800;
      letter-spacing: -0.3px;
      color: #fff;
    }

    .footer-desc {
      font-size: 0.85rem;
      line-height: 1.6;
      color: #94a3b8;
      margin: 0;
      max-width: 360px;
    }

    .footer-social {
      display: flex;
      gap: 8px;
      margin-top: 4px;
    }

    .social-link {
      width: 34px;
      height: 34px;
      border-radius: 8px;
      background: rgba(255,255,255,0.06);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: #94a3b8;
      cursor: pointer;
      transition: all 0.2s ease;
      text-decoration: none;
    }

    .social-link:hover {
      background: rgba(99, 102, 241, 0.2);
      color: #a5b4fc;
      transform: translateY(-2px);
    }

    .social-link svg {
      display: block;
    }

    .footer-heading {
      font-size: 0.8rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: #f1f5f9;
      margin: 0 0 6px;
    }

    .footer-ul {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .footer-ul li a {
      color: #94a3b8;
      text-decoration: none;
      font-size: 0.88rem;
      font-weight: 500;
      transition: color 0.15s ease;
    }

    .footer-ul li a:hover {
      color: #a5b4fc;
    }

    .footer-bottom {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 0;
      flex-wrap: wrap;
      gap: 8px;
    }

    .footer-copy {
      font-size: 0.8rem;
      color: #64748b;
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
    if (this.authService.isAuthenticated()) {
      this.cartService.getCart().subscribe({ error: () => { } });
    }
  }
}
