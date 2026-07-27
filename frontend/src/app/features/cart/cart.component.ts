import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';

import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { NotificationService } from '../../core/services/notification.service';
import { Cart, CartItem } from '../../core/models/cart.model';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { CheckoutDialogComponent } from './checkout-dialog/checkout-dialog.component';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule
  ],
  template: `
    <div class="cart-container">
      <!-- Header -->
      <div class="cart-page-header">
        <div>
          <h1 class="page-title text-gradient-cyan">
            <mat-icon class="title-icon">shopping_bag</mat-icon> Shopping Cart
          </h1>
          <p class="page-subtitle">Review items and manage quantities in your bag</p>
        </div>
      </div>

      <div *ngIf="loading" class="loading-container">
        <mat-spinner diameter="44"></mat-spinner>
      </div>

      <div *ngIf="!loading">
        <!-- Empty Cart View -->
        <div *ngIf="!cart || cart.items.length === 0" class="empty-cart-card glass-panel">
          <div class="empty-content">
            <mat-icon class="empty-icon">shopping_bag</mat-icon>
            <h2 class="text-gradient-cyan">Your Shopping Cart is Empty</h2>
            <p>Browse our storefront catalog to add items to your cart.</p>
            <a mat-raised-button class="btn-glowing explore-btn" routerLink="/products">
              <mat-icon>storefront</mat-icon> Browse Products
            </a>
          </div>
        </div>

        <!-- Active Cart Layout -->
        <div *ngIf="cart && cart.items.length > 0" class="cart-grid">
          
          <!-- Items List Column -->
          <div class="cart-items-card glass-panel">
            <div class="card-header">
              <h3>Cart Items ({{ cart.totalItems }})</h3>
              <button mat-button color="warn" class="clear-all-btn" (click)="onClearCart()" [disabled]="actionLoading">
                <mat-icon>delete_sweep</mat-icon> Clear Cart
              </button>
            </div>

            <div class="items-list">
              <div *ngFor="let item of cart.items" class="cart-item-row">
                <!-- Image -->
                <img [src]="item.imageUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500'"
                     (error)="onImageError($event)" [alt]="item.productName" class="item-img" />

                <!-- Details -->
                <div class="item-details">
                  <h4 class="item-name">{{ item.productName }}</h4>
                  <span class="item-unit-price">{{ item.unitPrice | currency:'USD':'symbol':'1.2-2' }}</span>
                </div>

                <!-- Quantity Picker -->
                <div class="quantity-picker">
                  <button mat-icon-button class="qty-btn" (click)="updateQuantity(item, item.quantity - 1)" [disabled]="actionLoading">
                    <mat-icon>remove</mat-icon>
                  </button>
                  <span class="qty-val">{{ item.quantity }}</span>
                  <button mat-icon-button class="qty-btn" (click)="updateQuantity(item, item.quantity + 1)" [disabled]="actionLoading">
                    <mat-icon>add</mat-icon>
                  </button>
                </div>

                <!-- Subtotal -->
                <div class="subtotal-box">
                  <strong class="subtotal-price text-gradient-cyan">{{ item.subtotal | currency:'USD':'symbol':'1.2-2' }}</strong>
                </div>

                <!-- Delete Action -->
                <button mat-icon-button color="warn" (click)="removeItem(item)" [disabled]="actionLoading" matTooltip="Remove item" class="delete-btn">
                  <mat-icon>delete_outline</mat-icon>
                </button>
              </div>
            </div>
          </div>

          <!-- Order Summary Sidebar -->
          <div class="summary-card glass-panel">
            <h3 class="summary-title text-gradient-cyan">Order Summary</h3>

            <div class="summary-body">
              <div class="summary-line">
                <span>Total Items</span>
                <strong>{{ cart.totalItems }} units</strong>
              </div>

              <div class="summary-divider"></div>

              <div class="summary-line total-line">
                <span>Total Amount</span>
                <strong class="total-price text-gradient-cyan">{{ cart.totalAmount | currency:'USD':'symbol':'1.2-2' }}</strong>
              </div>

              <!-- Checkout Action -->
              <button mat-raised-button class="btn-glowing checkout-btn" (click)="onCheckout()">
                <mat-icon>payment</mat-icon> Proceed to Checkout
              </button>
            </div>
          </div>

        </div>
      </div>
    </div>
  `,
  styles: [`
    .cart-container {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .cart-page-header {
      margin-bottom: 4px;
    }

    .page-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0;
      font-size: 2rem;
      font-weight: 800;
    }

    .title-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: #38bdf8;
    }

    .page-subtitle {
      margin: 4px 0 0 0;
      color: var(--text-muted);
      font-size: 0.9rem;
    }

    .loading-container {
      display: flex;
      justify-content: center;
      padding: 60px;
    }

    /* Empty Cart View */
    .empty-cart-card {
      padding: 56px 24px;
      max-width: 540px;
      margin: 20px auto;
      text-align: center;
    }

    .empty-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #38bdf8;
      margin-bottom: 16px;
    }

    .empty-content h2 {
      margin: 0 0 8px 0;
      font-size: 1.5rem;
      font-weight: 800;
    }

    .empty-content p {
      color: var(--text-muted);
      margin-bottom: 24px;
      font-size: 0.9rem;
    }

    .explore-btn {
      padding: 0 24px;
      height: 44px;
    }

    /* Active Cart Layout */
    .cart-grid {
      display: grid;
      grid-template-columns: minmax(0, 1.8fr) minmax(300px, 0.8fr);
      gap: 24px;
    }

    .cart-items-card {
      padding: 20px;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 14px;
      border-bottom: 1px solid var(--glass-border-subtle);
      margin-bottom: 16px;
    }

    .card-header h3 {
      margin: 0;
      font-size: 1.1rem;
      font-weight: 800;
      color: var(--text-main);
    }

    .clear-all-btn {
      font-size: 0.8rem;
    }

    .items-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .cart-item-row {
      display: grid;
      grid-template-columns: 56px 1.5fr auto 110px 40px;
      align-items: center;
      gap: 16px;
      padding: 12px 14px;
      border-radius: 14px;
      background: rgba(255, 255, 255, 0.7);
      border: 1px solid var(--glass-border-subtle);
      transition: all 0.2s ease;
    }

    .cart-item-row:hover {
      background: rgba(255, 255, 255, 0.95);
      border-color: var(--glass-border-glow);
    }

    .item-img {
      width: 56px;
      height: 56px;
      border-radius: 10px;
      object-fit: cover;
    }

    .item-details {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .item-name {
      margin: 0;
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--text-main);
    }

    .item-unit-price {
      font-size: 0.8rem;
      color: var(--text-muted);
      font-weight: 600;
    }

    /* Quantity Picker - Perfectly Centered Alignment */
    .quantity-picker {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      padding: 3px 6px;
      border-radius: 10px;
      background: #f1f5f9;
      border: 1px solid #cbd5e1;
    }

    .qty-btn {
      width: 28px !important;
      height: 28px !important;
      min-width: 28px !important;
      padding: 0 !important;
      display: inline-flex !important;
      align-items: center !important;
      justify-content: center !important;
      border-radius: 6px !important;
      color: #334155 !important;
      line-height: 1 !important;
    }

    .qty-btn ::ng-deep .mat-icon,
    .qty-btn mat-icon {
      font-size: 18px !important;
      width: 18px !important;
      height: 18px !important;
      line-height: 18px !important;
      display: inline-flex !important;
      align-items: center !important;
      justify-content: center !important;
      margin: 0 !important;
    }

    .qty-val {
      min-width: 28px;
      text-align: center;
      font-weight: 800;
      font-size: 0.95rem;
      color: var(--text-main);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      line-height: 1;
    }

    .subtotal-box {
      text-align: right;
    }

    .subtotal-price {
      font-size: 1.1rem;
      font-weight: 800;
    }

    .delete-btn {
      color: #94a3b8;
    }

    .delete-btn:hover {
      color: #ef4444;
    }

    /* Summary Sidebar */
    .summary-card {
      padding: 24px;
      height: fit-content;
      position: sticky;
      top: 90px;
    }

    .summary-title {
      margin: 0 0 16px 0;
      font-size: 1.2rem;
      font-weight: 800;
      padding-bottom: 12px;
      border-bottom: 1px solid var(--glass-border-subtle);
    }

    .summary-body {
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .summary-line {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 0.9rem;
      color: var(--text-secondary);
    }

    .summary-line strong {
      color: var(--text-main);
      font-size: 0.95rem;
    }

    .summary-divider {
      height: 1px;
      background: var(--glass-border-subtle);
      margin: 2px 0;
    }

    .total-line span {
      font-weight: 800;
      font-size: 1rem;
      color: var(--text-main);
    }

    .total-price {
      font-size: 1.6rem;
      font-weight: 900;
    }

    .checkout-btn {
      width: 100%;
      height: 48px;
      font-size: 1rem !important;
      margin-top: 8px;
    }

    @media (max-width: 900px) {
      .cart-grid {
        grid-template-columns: 1fr;
      }
      .summary-card {
        position: static;
      }
    }
  `]
})
export class CartComponent implements OnInit {
  cart: Cart | null = null;
  loading = false;
  actionLoading = false;

  constructor(
    private cartService: CartService,
    private orderService: OrderService,
    private notification: NotificationService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCart();
  }

  loadCart(): void {
    this.loading = true;
    this.cartService.getCart().subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.data) {
          this.cart = res.data;
        }
      },
      error: () => {
        this.loading = false;
        this.notification.error('Failed to load shopping cart');
      }
    });
  }

  updateQuantity(item: CartItem, newQuantity: number): void {
    if (newQuantity < 0) return;
    this.actionLoading = true;
    this.cartService.updateQuantity(item.productId, newQuantity).subscribe({
      next: (res) => {
        this.actionLoading = false;
        if (res.success && res.data) {
          this.cart = res.data;
        }
      },
      error: (err) => {
        this.actionLoading = false;
        this.notification.error(err.error?.message || 'Failed to update item quantity');
      }
    });
  }

  removeItem(item: CartItem): void {
    this.actionLoading = true;
    this.cartService.removeItem(item.productId).subscribe({
      next: () => {
        this.actionLoading = false;
        this.notification.success(`Removed "${item.productName}" from cart`);
        this.loadCart();
      },
      error: () => {
        this.actionLoading = false;
        this.notification.error('Failed to remove item');
      }
    });
  }

  onClearCart(): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Clear Shopping Cart',
        message: 'Are you sure you want to remove all items from your shopping bag?'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.actionLoading = true;
        this.cartService.clearCart().subscribe({
          next: () => {
            this.actionLoading = false;
            this.notification.success('Shopping cart cleared');
            this.loadCart();
          },
          error: () => {
            this.actionLoading = false;
            this.notification.error('Failed to clear cart');
          }
        });
      }
    });
  }

  onCheckout(): void {
    if (!this.cart || this.cart.items.length === 0) return;

    const dialogRef = this.dialog.open(CheckoutDialogComponent, {
      width: '500px',
      data: { cart: this.cart }
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.actionLoading = true;
        this.orderService.createOrder(result).subscribe({
          next: (res) => {
            this.actionLoading = false;
            this.notification.success(`Order #${res.data?.id || ''} placed successfully! Confirmation email has been dispatched.`);
            this.loadCart();
            this.router.navigate(['/orders']);
          },
          error: (err) => {
            this.actionLoading = false;
            this.notification.error(err.error?.message || 'Failed to place order. Please try again.');
          }
        });
      }
    });
  }

  onImageError(event: Event): void {
    (event.target as HTMLImageElement).src = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500';
  }
}
