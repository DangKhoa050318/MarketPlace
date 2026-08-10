import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';

import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { AnalyticsEventType } from '../../core/models/analytics-event.model';
import { AnalyticsService } from '../../core/services/analytics.service';
import { PromotionService } from '../../core/services/promotion.service';
import { NotificationService } from '../../core/services/notification.service';
import { Cart, CartItem } from '../../core/models/cart.model';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { CheckoutDialogComponent } from './checkout-dialog/checkout-dialog.component';
import { VietQrDialogComponent } from '../../shared/components/vietqr-dialog/vietqr-dialog.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    CheckoutDialogComponent,
    VietQrDialogComponent
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
                  <span class="item-variant" *ngIf="item.variantName">
                    <mat-icon class="variant-icon">lens</mat-icon>
                    {{ item.variantName }}
                  </span>
                  <span class="item-unit-price">{{ item.unitPrice | currency:'VND':'symbol':'1.0-0' }}</span>
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
                  <strong class="subtotal-price text-gradient-cyan">{{ item.subtotal | currency:'VND':'symbol':'1.0-0' }}</strong>
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

            <!-- Free Shipping Progress Banner -->
            <div *ngIf="getFreeShippingNeeded() > 0" class="shipping-progress-box">
              <div class="shipping-progress-text">
                <mat-icon class="shipping-icon">local_shipping</mat-icon>
                <span>Add <strong>{{ getFreeShippingNeeded() | currency:'VND':'symbol':'1.0-0' }}</strong> more for <strong>FREE Shipping</strong></span>
              </div>
              <div class="progress-track">
                <div class="progress-bar" [style.width.%]="shippingProgressPercent()"></div>
              </div>
            </div>

            <div *ngIf="getFreeShippingNeeded() === 0" class="shipping-progress-box qualified">
              <mat-icon class="shipping-icon">verified</mat-icon>
              <span>You unlocked <strong>FREE Shipping</strong>!</span>
            </div>

            <div class="summary-body">
              <div class="summary-line">
                <span>Subtotal</span>
                <strong>{{ cart.totalAmount | currency:'VND':'symbol':'1.0-0' }}</strong>
              </div>

              <!-- Coupon -->
              <div class="coupon-block">
                <div class="coupon-row" *ngIf="!appliedCode">
                  <mat-form-field appearance="outline" class="coupon-field" subscriptSizing="dynamic">
                    <mat-label>Coupon code</mat-label>
                    <input matInput [(ngModel)]="couponInput" placeholder="e.g. SUMMER10"
                           (keyup.enter)="applyCoupon()" />
                  </mat-form-field>
                  <button mat-stroked-button (click)="applyCoupon()" [disabled]="applyingCoupon || !couponInput">
                    <mat-spinner *ngIf="applyingCoupon" diameter="16"></mat-spinner>
                    <span *ngIf="!applyingCoupon">Apply</span>
                  </button>
                </div>
                <div class="coupon-applied" *ngIf="appliedCode">
                  <span><mat-icon class="ok">local_offer</mat-icon> {{ appliedCode }}</span>
                  <button mat-button color="warn" (click)="removeCoupon()">Remove</button>
                </div>
                <div class="coupon-error" *ngIf="couponError"><mat-icon>error_outline</mat-icon> {{ couponError }}</div>
              </div>

              <div class="summary-line discount-line" *ngIf="appliedCode">
                <span>Discount</span>
                <strong class="discount-val">− {{ discountAmount | currency:'VND':'symbol':'1.0-0' }}</strong>
              </div>

              <!-- Shipping Fee Line -->
              <div class="summary-line">
                <span>Shipping Fee</span>
                <strong *ngIf="getShippingFee() === 0" class="free-shipping-tag">FREE</strong>
                <strong *ngIf="getShippingFee() > 0">{{ getShippingFee() | currency:'VND':'symbol':'1.0-0' }}</strong>
              </div>

              <div class="summary-divider"></div>

              <div class="summary-line total-line">
                <span>Total Amount</span>
                <strong class="total-price text-gradient-cyan">{{ payableTotal() | currency:'VND':'symbol':'1.0-0' }}</strong>
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

    .item-variant {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 0.78rem;
      font-weight: 600;
      color: #6366f1;
      background: rgba(99, 102, 241, 0.1);
      padding: 1px 10px 1px 6px;
      border-radius: 20px;
      width: fit-content;
      margin: 2px 0 4px 0;
    }

    .variant-icon {
      font-size: 8px;
      width: 8px;
      height: 8px;
      line-height: 8px;
      color: #6366f1;
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

    .coupon-block {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .coupon-row {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .coupon-field {
      flex: 1;
    }

    .coupon-applied {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-weight: 700;
      color: #16a34a;
      font-size: 0.9rem;
    }

    .coupon-applied .ok {
      font-size: 18px;
      width: 18px;
      height: 18px;
      vertical-align: middle;
    }

    .coupon-error {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #ef4444;
      font-size: 0.8rem;
      font-weight: 600;
    }

    .coupon-error mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .discount-line .discount-val {
      color: #16a34a;
    }

    .free-shipping-tag {
      color: #10b981;
      font-weight: 800;
      background: #ecfdf5;
      padding: 1px 8px;
      border-radius: 6px;
      border: 1px solid rgba(16, 185, 129, 0.3);
      font-size: 0.8rem;
    }

    .shipping-progress-box {
      background: #f8fafc;
      border: 1px solid var(--border-subtle);
      border-radius: 12px;
      padding: 10px 14px;
      margin-bottom: 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .shipping-progress-box.qualified {
      background: #ecfdf5;
      border-color: rgba(16, 185, 129, 0.3);
      color: #047857;
      flex-direction: row;
      align-items: center;
      gap: 8px;
      font-size: 0.85rem;
      font-weight: 600;
    }

    .shipping-progress-text {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.83rem;
      color: #334155;
    }

    .shipping-icon {
      color: #4f46e5;
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .progress-track {
      height: 6px;
      background: #e2e8f0;
      border-radius: 4px;
      overflow: hidden;
    }

    .progress-bar {
      height: 100%;
      background: linear-gradient(90deg, #4f46e5, #10b981);
      border-radius: 4px;
      transition: width 0.3s ease;
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
export class CartComponent implements OnInit, OnDestroy {
  private readonly freeShippingThreshold = 3750000;
  private readonly standardShippingFee = 125000;
  private couponSubscription?: Subscription;
  cart: Cart | null = null;
  loading = false;
  actionLoading = false;

  couponInput = '';
  appliedCode: string | null = null;
  discountAmount = 0;
  couponError: string | null = null;
  applyingCoupon = false;

  constructor(
    private cartService: CartService,
    private orderService: OrderService,
    private promotionService: PromotionService,
    private analyticsService: AnalyticsService,
    private notification: NotificationService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  getShippingFee(): number {
    const subtotal = this.cart?.totalAmount ?? 0;
    if (subtotal === 0 || subtotal >= this.freeShippingThreshold) return 0;
    return this.standardShippingFee;
  }

  getFreeShippingNeeded(): number {
    const subtotal = this.cart?.totalAmount ?? 0;
    return Math.max(0, this.freeShippingThreshold - subtotal);
  }

  shippingProgressPercent(): number {
    const subtotal = this.cart?.totalAmount ?? 0;
    return Math.min(100, Math.round((subtotal / this.freeShippingThreshold) * 100));
  }

  payableTotal(): number {
    const subtotal = this.cart?.totalAmount ?? 0;
    const discount = this.appliedCode ? this.discountAmount : 0;
    const shipping = this.getShippingFee();
    return Math.max(0, subtotal - discount + shipping);
  }

  applyCoupon(): void {
    const code = (this.couponInput || '').trim();
    if (!code) return;
    this.applyingCoupon = true;
    this.couponError = null;
    this.promotionService.apply(code).subscribe({
      next: (res) => {
        this.applyingCoupon = false;
        const p = res.data;
        if (p?.valid) {
          this.couponError = null;
        } else {
          this.couponError = this.reasonMessage(p?.reason);
        }
      },
      error: (err) => {
        this.applyingCoupon = false;
        this.couponError = err.error?.message || 'Could not validate coupon';
      }
    });
  }

  removeCoupon(): void {
    this.promotionService.clearApplied();
    this.couponError = null;
    this.couponInput = '';
  }

  private reasonMessage(reason?: string): string {
    switch (reason) {
      case 'CODE_NOT_FOUND': return 'Coupon code not found';
      case 'INACTIVE': return 'This coupon is no longer active';
      case 'NOT_STARTED': return 'This coupon is not active yet';
      case 'EXPIRED': return 'This coupon has expired';
      case 'MIN_ORDER_NOT_MET': return 'Your cart does not meet the minimum order amount';
      case 'NO_ELIGIBLE_ITEMS': return 'No items in your cart qualify for this coupon';
      case 'USAGE_LIMIT_REACHED': return 'This coupon has reached its usage limit';
      case 'PER_USER_LIMIT_REACHED': return 'You have already used this coupon';
      default: return 'This coupon is not valid';
    }
  }

  ngOnInit(): void {
    this.couponSubscription = this.promotionService.appliedCoupon$.subscribe(preview => {
      this.appliedCode = preview?.code || null;
      this.discountAmount = preview?.discountAmount || 0;
      if (preview) this.couponInput = preview.code;
    });
    this.loadCart();
  }

  ngOnDestroy(): void {
    this.couponSubscription?.unsubscribe();
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
    this.cartService.updateQuantity(item.variantId, newQuantity).subscribe({
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
    this.cartService.removeItem(item.variantId).subscribe({
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
            this.removeCoupon();
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

    this.analyticsService.track(AnalyticsEventType.BeginCheckout, {
      totalItems: this.cart.totalItems,
      totalAmount: this.cart.totalAmount,
      items: this.cart.items.map(item => ({
        productName: item.productName,
        variantId: item.variantId,
        quantity: item.quantity,
        subtotal: item.subtotal
      }))
    });

    const dialogRef = this.dialog.open(CheckoutDialogComponent, {
      width: '620px',
      data: { cart: this.cart, couponCode: this.appliedCode, discountAmount: this.discountAmount }
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.actionLoading = true;
        this.orderService.createOrder(result).subscribe({
          next: (res) => {
            this.actionLoading = false;
            this.promotionService.clearApplied();
            this.analyticsService.track(AnalyticsEventType.OrderCreated, {
              orderId: res.data?.id,
              totalAmount: res.data?.totalAmount,
              itemCount: res.data?.items?.length
            });
            const targetUrl = res.data?.paygatePayload?.paymentUrl;
            if (targetUrl && (result.paymentMethod === 'CREDIT_CARD' || result.paymentMethod === 'PAYGATE_BNPL')) {
              this.notification.info(`Redirecting to PayGate Checkout...`);
              this.loadCart();
              window.location.href = targetUrl;
            } else if (result.paymentMethod === 'BANK_TRANSFER') {
              this.notification.success(`Order #${res.data?.id || ''} placed successfully! Please complete VietQR payment.`);
              this.loadCart();
              const vndAmount = Math.round(res.data?.totalAmount || 0);
              const pg = res.data?.paygatePayload;
              let timerSecs = 900;
              if (res.data?.paygateExpiresAt) {
                const expires = new Date(res.data.paygateExpiresAt).getTime();
                const now = new Date().getTime();
                const diff = Math.floor((expires - now) / 1000);
                timerSecs = diff > 0 ? diff : 0;
              }
              const dialogRef = this.dialog.open(VietQrDialogComponent, {
                width: '840px',
                maxWidth: '95vw',
                panelClass: 'paygate-vqr-dialog-panel',
                disableClose: false,
                data: {
                  orderId: res.data?.id,
                  amountVnd: vndAmount,
                  description: pg?.transferContent || `ORD-${res.data?.id}`,
                  qrPayload: pg?.qrPayload,
                  bankName: pg?.bankAccount?.bankName,
                  accountNo: pg?.bankAccount?.accountNumber,
                  accountName: pg?.bankAccount?.accountHolder,
                  timerSeconds: timerSecs
                }
              });
              dialogRef.afterClosed().subscribe((result) => {
                if (result === true && res.data?.id) {
                  const orderId = res.data.id;
                  this.orderService.confirmVietQrPayment(orderId).subscribe({
                    next: () => {
                      this.notification.success('Payment confirmed! Your order is now CONFIRMED.');
                      this.router.navigate(['/orders', orderId]);
                    },
                    error: () => {
                      this.router.navigate(['/orders', orderId]);
                    }
                  });
                } else if (result === 'CANCEL' && res.data?.id) {
                  const orderId = res.data.id;
                  this.orderService.cancelVietQrPayment(orderId).subscribe({
                    next: () => {
                      this.notification.info('Payment cancelled. Reserved stock has been released.');
                      this.router.navigate(['/orders', orderId]);
                    },
                    error: () => {
                      this.router.navigate(['/orders', orderId]);
                    }
                  });
                } else if (res.data?.id) {
                  this.router.navigate(['/orders', res.data.id]);
                } else {
                  this.router.navigate(['/orders']);
                }
              });
            } else {
              this.notification.success(`Order #${res.data?.id || ''} placed successfully! Confirmation email has been dispatched.`);
              this.loadCart();
              this.router.navigate(['/orders']);
            }
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
