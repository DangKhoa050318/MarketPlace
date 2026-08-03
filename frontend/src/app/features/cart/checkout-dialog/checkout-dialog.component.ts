import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Cart } from '../../../core/models/cart.model';

export interface CheckoutDialogData {
  cart: Cart;
  couponCode?: string | null;   // applied on the cart page (F-306); shown read-only here
  discountAmount?: number;
}

@Component({
  selector: 'app-checkout-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <div class="checkout-modal-container">
      <div class="modal-header">
        <h2 class="dialog-title text-gradient-cyan">
          <mat-icon class="title-icon">local_shipping</mat-icon> Checkout
        </h2>
        <button mat-icon-button (click)="onCancel()" class="close-btn"><mat-icon>close</mat-icon></button>
      </div>

      <mat-dialog-content class="dialog-content">
        <!-- Order Summary Box -->
        <div class="checkout-summary-box glass-panel">
          <div class="summary-item">
            <span class="label">Subtotal ({{ data.cart.totalItems }} items)</span>
            <strong class="val">{{ data.cart.totalAmount | currency:'USD':'symbol':'1.2-2' }}</strong>
            <span class="shipping-note" *ngIf="shippingFee() === 0">Shipping: <strong>FREE</strong></span>
            <span class="shipping-note" *ngIf="shippingFee() > 0">Shipping: <strong>{{ shippingFee() | currency:'USD':'symbol':'1.2-2' }}</strong></span>
          </div>
          <div class="summary-item align-right">
            <span class="label">Total Payment</span>
            <span *ngIf="data.couponCode" class="strike">{{ (data.cart.totalAmount + shippingFee()) | currency:'USD':'symbol':'1.2-2' }}</span>
            <strong class="total-amount text-gradient-cyan">{{ payableTotal() | currency:'USD':'symbol':'1.2-2' }}</strong>
          </div>
        </div>

        <!-- Applied coupon (read-only; entered on the cart page) -->
        <div *ngIf="data.couponCode" class="coupon-applied-line">
          <mat-icon>local_offer</mat-icon>
          Coupon <strong>{{ data.couponCode }}</strong> applied — you save
          {{ (data.discountAmount || 0) | currency:'USD':'symbol':'1.2-2' }}.
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="checkout-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Shipping Address</mat-label>
            <input matInput formControlName="shippingAddress" placeholder="Enter your delivery address..." required />
            <mat-icon matSuffix>location_on</mat-icon>
            <mat-error *ngIf="form.get('shippingAddress')?.hasError('required')">
              Shipping address is required
            </mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Order Notes (Optional)</mat-label>
            <textarea matInput formControlName="note" rows="2" placeholder="Notes for delivery..."></textarea>
            <mat-icon matSuffix>note_add</mat-icon>
          </mat-form-field>
        </form>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="dialog-actions">
        <button mat-button (click)="onCancel()" [disabled]="submitting">Cancel</button>
        <button
          mat-raised-button
          class="btn-glowing"
          (click)="onSubmit()"
          [disabled]="form.invalid || submitting">
          <mat-icon>shopping_bag</mat-icon> {{ submitting ? 'Processing...' : 'Confirm Order' }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .checkout-modal-container {
      padding: 4px;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px 8px;
      border-bottom: 1px solid var(--glass-border-subtle);
    }

    .dialog-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 1.3rem;
      font-weight: 800;
      margin: 0;
    }

    .title-icon {
      font-size: 24px;
      width: 24px;
      height: 24px;
      color: #38bdf8;
    }

    .close-btn {
      color: #94a3b8;
    }

    .dialog-content {
      padding: 16px 20px !important;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .checkout-summary-box {
      padding: 14px 18px;
      border-radius: 14px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: rgba(99, 102, 241, 0.06);
      border: 1px solid rgba(99, 102, 241, 0.15);
    }

    .summary-item {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .summary-item.align-right {
      align-items: flex-end;
    }

    .summary-item .label {
      font-size: 0.72rem;
      color: var(--text-muted);
      text-transform: uppercase;
      font-weight: 700;
    }

    .summary-item .val {
      font-size: 0.95rem;
      color: var(--text-main);
    }

    .total-amount {
      font-size: 1.35rem;
      font-weight: 900;
    }

    .shipping-note {
      font-size: 0.78rem;
      color: var(--text-muted);
      margin-top: 2px;
    }

    .shipping-note strong {
      color: #10b981;
    }

    .strike {
      text-decoration: line-through;
      color: var(--text-muted);
      font-size: 0.8rem;
      margin-right: 8px;
    }

    .coupon-applied-line {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.85rem;
      font-weight: 600;
      color: #16a34a;
    }

    .coupon-applied-line mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .checkout-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .full-width {
      width: 100%;
    }

    .dialog-actions {
      padding: 12px 20px 16px !important;
      display: flex;
      gap: 10px;
    }
  `]
})
export class CheckoutDialogComponent {
  form: FormGroup;
  submitting = false;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<CheckoutDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CheckoutDialogData
  ) {
    this.form = this.fb.group({
      shippingAddress: ['', [Validators.required, Validators.minLength(5)]],
      note: ['']
    });
  }

  shippingFee(): number {
    const subtotal = this.data.cart.totalAmount ?? 0;
    if (subtotal === 0 || subtotal >= 150) return 0;
    return 5;
  }

  payableTotal(): number {
    const subtotal = this.data.cart.totalAmount ?? 0;
    const discount = this.data.couponCode ? (this.data.discountAmount || 0) : 0;
    const shipping = this.shippingFee();
    return Math.max(0, subtotal - discount + shipping);
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSubmit(): void {
    if (this.form.valid) {
      this.dialogRef.close({
        shippingAddress: this.form.value.shippingAddress,
        note: this.form.value.note,
        couponCode: this.data.couponCode || undefined
      });
    }
  }
}
