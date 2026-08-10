import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { Cart } from '../../../core/models/cart.model';
import { PaymentMethod } from '../../../core/models/order.model';
import { environment } from '../../../../environments/environment';

export interface CheckoutDialogData {
  cart: Cart;
  couponCode?: string | null;
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
    MatIconModule,
    MatSelectModule
  ],
  template: `
    <div class="checkout-modal-container">
      <div class="modal-header">
        <h2 class="dialog-title text-gradient-cyan">
          <mat-icon class="title-icon">shopping_cart_checkout</mat-icon> Production Checkout & Payment
        </h2>
        <button mat-icon-button (click)="onCancel()" class="close-btn"><mat-icon>close</mat-icon></button>
      </div>

      <mat-dialog-content class="dialog-content">
        <!-- Order Summary Box -->
        <div class="checkout-summary-box glass-panel">
          <div class="summary-item">
            <span class="label">Subtotal ({{ data.cart.totalItems }} items)</span>
            <strong class="val">{{ data.cart.totalAmount | currency:'VND':'symbol':'1.0-0' }}</strong>
            <span class="shipping-note" *ngIf="shippingFee() === 0">Shipping: <strong>FREE</strong></span>
            <span class="shipping-note" *ngIf="shippingFee() > 0">Shipping: <strong>{{ shippingFee() | currency:'VND':'symbol':'1.0-0' }}</strong></span>
          </div>
          <div class="summary-item align-right">
            <span class="label">Total Payable</span>
            <span *ngIf="data.couponCode" class="strike">{{ (data.cart.totalAmount + shippingFee()) | currency:'VND':'symbol':'1.0-0' }}</span>
            <strong class="total-amount text-gradient-cyan">{{ payableTotal() | currency:'VND':'symbol':'1.0-0' }}</strong>
          </div>
        </div>

        <!-- Applied Coupon -->
        <div *ngIf="data.couponCode" class="coupon-applied-line">
          <mat-icon>local_offer</mat-icon>
          Coupon <strong>{{ data.couponCode }}</strong> applied — Savings:
          {{ (data.discountAmount || 0) | currency:'VND':'symbol':'1.0-0' }}.
        </div>

        <!-- Payment Method Options -->
        <div class="payment-section">
          <h3 class="section-heading"><mat-icon>payments</mat-icon> Select Payment Method</h3>
          <div class="payment-options-grid">
            <!-- Option 1: COD -->
            <div
              class="payment-card"
              [class.active]="selectedMethod === 'COD'"
              (click)="selectMethod('COD')">
              <div class="radio-indicator"></div>
              <mat-icon class="method-icon cod-icon">local_post_office</mat-icon>
              <div class="method-details">
                <span class="method-title">Cash on Delivery (COD)</span>
                <span class="method-desc">Pay cash upon receiving items. Instant order confirmation.</span>
              </div>
              <span class="badge-recommended">Default</span>
            </div>

            <!-- Option 2: PayGate E-Wallet / Card Gateway -->
            <div
              class="payment-card"
              [class.active]="selectedMethod === 'CREDIT_CARD'"
              (click)="selectMethod('CREDIT_CARD')">
              <div class="radio-indicator"></div>
              <mat-icon class="method-icon card-icon">account_balance_wallet</mat-icon>
              <div class="method-details">
                <span class="method-title">PayGate E-Wallet / Card Gateway</span>
                <span class="method-desc">Pay online securely via PayGate portal with OTP verification.</span>
              </div>
            </div>

            <!-- Option 3: Marketplace Wallet -->
            <div
              class="payment-card"
              [class.active]="selectedMethod === 'WALLET'"
              (click)="selectMethod('WALLET')">
              <div class="radio-indicator"></div>
              <mat-icon class="method-icon wallet-icon">account_balance_wallet</mat-icon>
              <div class="method-details">
                <span class="method-title">Marketplace Wallet</span>
                <span class="method-desc">Use credit from cancelled BNPL orders for this purchase.</span>
              </div>
              <span class="badge-bank">Credit</span>
            </div>

            <!-- Option 4: BNPL (Buy Now Pay Later) -->
            <div
              class="payment-card"
              [class.active]="selectedMethod === 'PAYGATE_BNPL'"
              (click)="selectMethod('PAYGATE_BNPL')">
              <div class="radio-indicator"></div>
              <mat-icon class="method-icon bnpl-icon">event_repeat</mat-icon>
              <div class="method-details">
                <span class="method-title">Buy Now Pay Later</span>
                <span class="method-desc">Choose upfront amount here, borrow the remaining amount through PayGate.</span>
              </div>
              <span class="badge-promo">0% Interest</span>
            </div>

            <!-- Option 5: Bank Transfer / QR VietQR -->
            <div
              class="payment-card"
              [class.active]="selectedMethod === 'BANK_TRANSFER'"
              (click)="selectMethod('BANK_TRANSFER')">
              <div class="radio-indicator"></div>
              <mat-icon class="method-icon bank-icon">qr_code_2</mat-icon>
              <div class="method-details">
                <span class="method-title">Bank Transfer / QR VietQR</span>
                <span class="method-desc">Transfer directly via VietQR / Mobile Banking to PayGate master account.</span>
              </div>
              <span class="badge-promo">No Fee</span>
            </div>
          </div>
        </div>

        <div *ngIf="selectedMethod === 'PAYGATE_BNPL'" class="bnpl-details-box fade-in">
          <div class="bnpl-term-header">
            <span>BNPL split before PayGate</span>
            <div class="amount-stack">
              <strong>{{ payableTotal() | currency:'VND':'symbol':'1.0-0' }}</strong>
            </div>
          </div>

          <div class="bnpl-input-grid">
            <mat-form-field appearance="outline">
              <mat-label>Upfront amount</mat-label>
              <input matInput type="number" min="0" [max]="payableTotal()" step="0.01" [value]="bnplUpfrontAmount" (input)="onBnplUpfrontInput($event)" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Finance amount</mat-label>
              <input matInput type="number" min="0.01" [max]="payableTotal()" step="0.01" [value]="financeAmount()" (input)="onBnplFinanceInput($event)" />
            </mat-form-field>
          </div>

          <div class="bnpl-breakdown-grid">
            <div class="breakdown-col">
              <span class="b-label">Pay now</span>
              <strong class="b-val text-amber">{{ upfrontAmount() | currency:'VND':'symbol':'1.0-0' }}</strong>
            </div>
            <div class="breakdown-col">
              <span class="b-label">Borrow via PayGate</span>
              <strong class="b-val text-cyan">{{ financeAmount() | currency:'VND':'symbol':'1.0-0' }}</strong>
            </div>
          </div>
        </div>

        <!-- Form for Shipping & Note -->
        <div class="delivery-section">
          <h3 class="section-heading"><mat-icon class="heading-icon text-cyan">local_shipping</mat-icon> Delivery Information</h3>
          <form [formGroup]="form" class="checkout-form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Shipping Address</mat-label>
              <input matInput formControlName="shippingAddress" placeholder="Enter your full delivery address..." required />
              <mat-icon matSuffix class="text-cyan">location_on</mat-icon>
              <mat-error *ngIf="form.get('shippingAddress')?.hasError('required')">
                Shipping address is required
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Order Notes (Optional)</mat-label>
              <textarea matInput formControlName="note" rows="2" placeholder="Instructions for delivery driver..."></textarea>
              <mat-icon matSuffix class="text-cyan">note_add</mat-icon>
            </mat-form-field>
          </form>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="dialog-actions">
        <button mat-button (click)="onCancel()" [disabled]="submitting">Cancel</button>
        <button
          mat-raised-button
          class="btn-glowing"
          (click)="onSubmit()"
          [disabled]="form.invalid || submitting || isBnplSplitInvalid()">
          <mat-icon>{{ selectedMethod === 'COD' ? 'shopping_bag' : (selectedMethod === 'BANK_TRANSFER' ? 'qr_code_2' : (selectedMethod === 'WALLET' ? 'account_balance_wallet' : 'open_in_new')) }}</mat-icon>
          {{ submitting ? 'Processing...' : (selectedMethod === 'COD' ? 'Confirm COD Order' : (selectedMethod === 'BANK_TRANSFER' ? 'Confirm & Show VietQR Code' : (selectedMethod === 'WALLET' ? 'Pay with Marketplace Wallet' : 'Proceed to PayGate Portal'))) }}
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
      padding: 16px 20px 10px;
      border-bottom: 1px solid var(--glass-border-subtle);
    }

    .dialog-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 1.25rem;
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
      max-height: 75vh;
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

    .section-heading {
      font-size: 0.95rem;
      font-weight: 700;
      color: #334155;
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 4px 0 10px;
    }

    .payment-options-grid {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .payment-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      border-radius: 12px;
      border: 2px solid #e2e8f0;
      background: #ffffff;
      cursor: pointer;
      transition: all 0.2s ease;
      position: relative;
    }

    .payment-card:hover {
      border-color: #cbd5e1;
      background: #f8fafc;
    }

    .payment-card.active {
      border-color: #0284c7;
      background: #f0f9ff;
      box-shadow: 0 4px 12px rgba(2, 132, 199, 0.12);
    }

    .radio-indicator {
      width: 18px;
      height: 18px;
      border-radius: 50%;
      border: 2px solid #cbd5e1;
      position: relative;
    }

    .payment-card.active .radio-indicator {
      border-color: #0284c7;
      background: #0284c7;
    }

    .payment-card.active .radio-indicator::after {
      content: '';
      position: absolute;
      top: 4px;
      left: 4px;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #ffffff;
    }

    .method-icon {
      font-size: 24px;
      width: 24px;
      height: 24px;
    }
    .cod-icon { color: #16a34a; }
    .card-icon { color: #0284c7; }
    .wallet-icon { color: #7c3aed; }
    .bnpl-icon { color: #d97706; }
    .bank-icon { color: #0284c7; }

    .method-details {
      display: flex;
      flex-direction: column;
      flex: 1;
    }

    .method-title {
      font-weight: 700;
      font-size: 0.92rem;
      color: #0f172a;
    }

    .method-desc {
      font-size: 0.78rem;
      color: #64748b;
    }

    .badge-recommended, .badge-promo, .badge-bank {
      font-size: 0.7rem;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 12px;
      text-transform: uppercase;
    }
    .badge-recommended { background: #dcfce7; color: #15803d; }
    .badge-promo { background: #fef3c7; color: #b45309; }
    .badge-bank { background: #e0f2fe; color: #0369a1; }

    .bnpl-details-box {
      margin-top: 10px;
      padding: 14px 16px;
      border-radius: 12px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
    }

    .bnpl-term-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 0.85rem;
      font-weight: 600;
      color: #334155;
      margin-bottom: 10px;
    }

    .amount-stack {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 2px;
    }

    .amount-stack small,
    .breakdown-col small {
      color: #64748b;
      font-size: 0.72rem;
      font-weight: 600;
    }

    .term-pills {
      display: flex;
      gap: 6px;
    }

    .term-pill {
      padding: 4px 10px;
      border-radius: 8px;
      border: 1px solid #cbd5e1;
      background: #ffffff;
      font-size: 0.78rem;
      font-weight: 700;
      color: #475569;
      cursor: pointer;
    }

    .term-pill.selected {
      border-color: #d97706;
      background: #fef3c7;
      color: #b45309;
    }

    .bnpl-breakdown-grid {
      display: flex;
      justify-content: space-between;
      background: #ffffff;
      padding: 10px 14px;
      border-radius: 10px;
      border: 1px solid #e2e8f0;
    }

    .bnpl-input-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 10px;
    }

    .breakdown-col {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .b-label {
      font-size: 0.7rem;
      color: #64748b;
      font-weight: 600;
    }

    .b-val {
      font-size: 0.9rem;
    }

    .text-amber { color: #d97706; }
    .text-cyan { color: #0284c7; }

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

    .paygate-spec-preview {
      margin-top: 4px;
    }

    .preview-toggle-btn {
      background: none;
      border: none;
      color: #0284c7;
      font-size: 0.8rem;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 0;
    }

    .payload-code-box {
      margin-top: 8px;
      background: #0f172a;
      color: #38bdf8;
      padding: 10px 14px;
      border-radius: 8px;
      font-family: monospace;
      font-size: 0.75rem;
      overflow-x: auto;
    }

    .credit-limit-banner {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      background: rgba(2, 132, 199, 0.08);
      border-radius: 8px;
      font-size: 0.78rem;
      color: #0369a1;
      margin-bottom: 10px;
    }

    .limit-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: #0284c7;
    }

    .fade-in {
      animation: fadeIn 0.25s ease-in-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-4px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class CheckoutDialogComponent {
  private readonly freeShippingThreshold = 3750000;
  private readonly standardShippingFee = 125000;
  form: FormGroup;
  selectedMethod: PaymentMethod = 'COD';
  selectedBnplMonths = 3;
  bnplUpfrontAmount = 0;
  showPaygateSpec = false;
  submitting = false;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<CheckoutDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CheckoutDialogData
  ) {
    this.form = this.fb.group({
      shippingAddress: ['123 Main St, San Jose, CA 95112', [Validators.required, Validators.minLength(5)]],
      note: ['']
    });
  }

  selectMethod(method: PaymentMethod): void {
    this.selectedMethod = method;
    if (method === 'PAYGATE_BNPL') {
      this.bnplUpfrontAmount = this.upfrontAmount();
    }
  }

  shippingFee(): number {
    const subtotal = this.data.cart.totalAmount ?? 0;
    if (subtotal === 0 || subtotal >= this.freeShippingThreshold) return 0;
    return this.standardShippingFee;
  }

  payableTotal(): number {
    const subtotal = this.data.cart.totalAmount ?? 0;
    const discount = this.data.couponCode ? (this.data.discountAmount || 0) : 0;
    const shipping = this.shippingFee();
    return Math.max(0, subtotal - discount + shipping);
  }

  financeAmount(): number {
    const total = this.payableTotal();
    if (this.selectedMethod !== 'PAYGATE_BNPL') {
      return 0;
    }
    return this.roundMoney(total - this.upfrontAmount());
  }

  upfrontAmount(): number {
    const total = this.payableTotal();
    if (this.selectedMethod !== 'PAYGATE_BNPL') {
      return total;
    }
    return this.roundMoney(Math.min(Math.max(this.bnplUpfrontAmount || 0, 0), total));
  }

  onBnplUpfrontInput(event: Event): void {
    this.bnplUpfrontAmount = this.clampMoney(Number((event.target as HTMLInputElement).value || 0));
  }

  onBnplFinanceInput(event: Event): void {
    const total = this.payableTotal();
    const finance = this.clampMoney(Number((event.target as HTMLInputElement).value || 0));
    this.bnplUpfrontAmount = this.roundMoney(total - finance);
  }

  monthlyPayment(): number {
    const financed = this.financeAmount();
    if (this.selectedBnplMonths <= 0 || financed <= 0) return 0;
    return Math.round((financed / this.selectedBnplMonths) * 100) / 100;
  }

  isBnplSplitInvalid(): boolean {
    if (this.selectedMethod !== 'PAYGATE_BNPL') return false;
    return this.financeAmount() <= 0 || this.upfrontAmount() + this.financeAmount() !== this.roundMoney(this.payableTotal());
  }

  getPayloadPreviewJson(): string {
    if (this.selectedMethod === 'PAYGATE_BNPL') {
      return JSON.stringify({
        apiKey: 'mock-merchant-api-key-123456',
        orderId: 'ORD_AUTO_GEN',
        paymentMethod: 'PAYGATE_BNPL',
        paymentType: 'BNPL_FINANCING',
        amount: Math.round(this.payableTotal()),
        upfrontAmount: Math.round(this.upfrontAmount()),
        financeAmount: Math.round(this.financeAmount()),
        currency: 'VND',
        bnplMonths: this.selectedBnplMonths,
        monthlyPayment: this.monthlyPayment(),
        description: `Tra gop PayGate BNPL (${this.selectedBnplMonths} thang) cho don hang Marketplace`,
        returnUrl: `${environment.appBaseUrl}/orders/callback?status=SUCCESS`,
        cancelUrl: `${environment.appBaseUrl}/orders/callback?status=CANCELLED`
      }, null, 2);
    } else if (this.selectedMethod === 'CREDIT_CARD') {
      return JSON.stringify({
        apiKey: 'mock-merchant-api-key-123456',
        orderId: 'ORD_AUTO_GEN',
        paymentMethod: 'CREDIT_CARD',
        paymentType: 'FULL_PAYMENT',
        amount: this.payableTotal(),
        description: 'Thanh toan 100% qua PayGate E-Wallet / Card Gateway',
        returnUrl: `${environment.appBaseUrl}/orders/callback?status=SUCCESS`,
        cancelUrl: `${environment.appBaseUrl}/orders/callback?status=CANCELLED`
      }, null, 2);
    } else if (this.selectedMethod === 'WALLET') {
      return JSON.stringify({
        shippingAddress: this.form.value.shippingAddress || '123 Delivery Street',
        note: this.form.value.note || '',
        paymentMethod: 'WALLET',
        couponCode: this.data.couponCode || null,
        walletCharge: this.payableTotal()
      }, null, 2);
    } else {
      return JSON.stringify({
        shippingAddress: this.form.value.shippingAddress || '123 Delivery Street',
        note: this.form.value.note || '',
        paymentMethod: 'COD',
        couponCode: this.data.couponCode || null
      }, null, 2);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSubmit(): void {
    if (this.form.valid) {
      this.dialogRef.close({
        shippingAddress: this.form.value.shippingAddress,
        note: this.form.value.note,
        couponCode: this.data.couponCode || undefined,
        paymentMethod: this.selectedMethod,
        upfrontAmount: this.upfrontAmount(),
        financeAmount: this.financeAmount(),
        bnplMonths: this.selectedMethod === 'PAYGATE_BNPL' ? this.selectedBnplMonths : undefined
      });
    }
  }

  private clampMoney(amount: number): number {
    return this.roundMoney(Math.min(Math.max(amount, 0), this.payableTotal()));
  }

  private roundMoney(amount: number): number {
    return Math.round(amount * 100) / 100;
  }
}
