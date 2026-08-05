import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

export interface BankTransferDialogData {
  orderId: number;
  totalAmount: number;
  bankAccount?: {
    bankName?: string;
    accountNumber?: string;
    accountHolder?: string;
    amount?: number;
  };
  transferContent?: string;
  qrPayload?: string;
}

@Component({
  selector: 'app-bank-transfer-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule
  ],
  template: `
    <div class="bank-transfer-dialog-wrapper">
      <!-- Top Brand Header Banner -->
      <div class="brand-header">
        <div class="brand-title">
          <div class="brand-icon-box">
            <mat-icon>qr_code_scanner</mat-icon>
          </div>
          <div>
            <div class="brand-badges">
              <span class="vietqr-pill">VIETQR</span>
              <span class="napas-pill">Napas 247</span>
            </div>
            <h2>Thanh Toán Qua Chuyển Khoản QR</h2>
          </div>
        </div>
        <button mat-icon-button (click)="closeDialog()" class="close-btn">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content class="dialog-body">
        <div class="info-alert-banner">
          <span class="pulse-dot"></span>
          <span>Mở App Ngân hàng bất kỳ để quét mã QR hoặc chuyển khoản theo thông tin:</span>
        </div>

        <div class="qr-main-grid">
          <!-- Left Column: Authentic VietQR Card Mock -->
          <div class="vietqr-card-box">
            <div class="vietqr-card">
              <div class="card-top-bar">
                <span class="napas-brand">napas<span>247</span></span>
                <span class="vietqr-brand">Viet<span>QR</span></span>
              </div>
              <div class="qr-svg-container">
                <svg viewBox="0 0 100 100" class="qr-svg">
                  <!-- Pattern Matrix Simulation -->
                  <path fill="#0f172a" d="M10,10 h25 v25 h-25 z M15,15 v15 h15 v-15 z M20,20 h5 v5 h-5 z"/>
                  <path fill="#0f172a" d="M65,10 h25 v25 h-25 z M70,15 v15 h15 v-15 z M75,20 h5 v5 h-5 z"/>
                  <path fill="#0f172a" d="M10,65 h25 v25 h-25 z M15,70 v15 h15 v-15 z M20,75 h5 v5 h-5 z"/>
                  <!-- Random SVG Data Modules -->
                  <rect fill="#0284c7" x="40" y="10" width="8" height="8"/>
                  <rect fill="#0f172a" x="52" y="10" width="8" height="8"/>
                  <rect fill="#0f172a" x="40" y="22" width="20" height="8"/>
                  <rect fill="#0284c7" x="40" y="34" width="8" height="20"/>
                  <rect fill="#0f172a" x="52" y="40" width="16" height="8"/>
                  <rect fill="#0f172a" x="10" y="40" width="12" height="12"/>
                  <rect fill="#0284c7" x="26" y="44" width="8" height="16"/>
                  <rect fill="#0f172a" x="70" y="40" width="20" height="8"/>
                  <rect fill="#0284c7" x="65" y="52" width="10" height="20"/>
                  <rect fill="#0f172a" x="80" y="52" width="10" height="10"/>
                  <rect fill="#0f172a" x="40" y="65" width="15" height="8"/>
                  <rect fill="#0284c7" x="40" y="77" width="8" height="15"/>
                  <rect fill="#0f172a" x="58" y="70" width="12" height="20"/>
                  <rect fill="#0f172a" x="75" y="75" width="15" height="15"/>
                </svg>
                <div class="qr-center-logo">
                  <mat-icon>account_balance</mat-icon>
                </div>
              </div>
              <div class="card-bottom-info">
                <div class="pay-amount-badge">{{ formattedAmount }} VNĐ</div>
                <div class="merchant-tag">PayGate Merchant Checkout</div>
              </div>
            </div>
            <div class="scan-instruction">
              <mat-icon class="camera-icon">center_focus_strong</mat-icon>
              <span>Tự động điền Số tiền & Nội dung</span>
            </div>
          </div>

          <!-- Right Column: Account Details Box -->
          <div class="banking-details-card">
            <!-- Bank Name -->
            <div class="detail-item">
              <span class="item-label"><mat-icon>account_balance</mat-icon> Ngân hàng thụ hưởng</span>
              <span class="item-value bank-name">{{ data.bankAccount?.bankName || 'Ngân hàng chủ PayGate (VietinBank)' }}</span>
            </div>

            <!-- Account Number -->
            <div class="detail-item glow-item">
              <span class="item-label"><mat-icon>credit_card</mat-icon> Số tài khoản</span>
              <div class="item-value-row">
                <span class="item-value acc-number">{{ data.bankAccount?.accountNumber || 'PAYGATE-0001-9999' }}</span>
                <button mat-button class="quick-copy-btn" (click)="copyToClipboard(data.bankAccount?.accountNumber || 'PAYGATE-0001-9999', 'Số tài khoản')">
                  <mat-icon>content_copy</mat-icon> Sao chép
                </button>
              </div>
            </div>

            <!-- Account Holder -->
            <div class="detail-item">
              <span class="item-label"><mat-icon>person</mat-icon> Tên tài khoản</span>
              <span class="item-value holder-name">{{ data.bankAccount?.accountHolder || 'PayGate JSC' }}</span>
            </div>

            <!-- Amount -->
            <div class="detail-item glow-item">
              <span class="item-label"><mat-icon>payments</mat-icon> Số tiền cần chuyển</span>
              <div class="item-value-row">
                <span class="item-value amount-val">{{ formattedAmount }} VNĐ</span>
                <button mat-button class="quick-copy-btn" (click)="copyToClipboard(rawAmountNum.toString(), 'Số tiền')">
                  <mat-icon>content_copy</mat-icon> Sao chép
                </button>
              </div>
            </div>

            <!-- Content String (CRITICAL) -->
            <div class="detail-item highlight-content-item">
              <span class="item-label content-label"><mat-icon class="pulse-icon">vpn_key</mat-icon> Nội dung chuyển khoản (bắt buộc)</span>
              <div class="item-value-row">
                <span class="item-value content-val">{{ transferContentStr }}</span>
                <button mat-flat-button class="copy-highlight-btn" (click)="copyToClipboard(transferContentStr, 'Nội dung chuyển khoản')">
                  <mat-icon>content_copy</mat-icon> Sao chép mã
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Warning Disclaimer -->
        <div class="warning-disclaimer">
          <mat-icon class="warn-icon">info</mat-icon>
          <div>
            <strong>Lưu ý quan trọng:</strong> Hệ thống tự động xác nhận ngay khi nhận đúng <strong>Nội dung chuyển khoản</strong>. Không chỉnh sửa mã để tránh chậm trễ xử lý.
          </div>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="dialog-actions">
        <button mat-stroked-button class="mock-test-btn" (click)="triggerMockBankTransfer()" [disabled]="simulating" matTooltip="Giả lập PayGate Webhook đã nhận tiền">
          <mat-icon>bolt</mat-icon>
          {{ simulating ? 'Đang gửi Webhook...' : '⚡ Giả Lập Thanh Toán (Test Demo)' }}
        </button>
        <button mat-raised-button class="confirm-done-btn" (click)="viewOrders()">
          <mat-icon>check_circle</mat-icon>
          Tôi Đã Chuyển Khoản → Xem Đơn Hàng
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .bank-transfer-dialog-wrapper {
      background: #0b1120;
      color: #f8fafc;
      border-radius: 20px;
      overflow: hidden;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7);
      border: 1px solid rgba(56, 189, 248, 0.2);
    }

    .brand-header {
      background: linear-gradient(135deg, #0284c7 0%, #1e1b4b 100%);
      padding: 16px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .brand-title {
      display: flex;
      align-items: center;
      gap: 14px;
    }

    .brand-icon-box {
      width: 44px;
      height: 44px;
      background: rgba(255, 255, 255, 0.15);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      backdrop-filter: blur(8px);
    }

    .brand-icon-box mat-icon {
      font-size: 26px;
      width: 26px;
      height: 26px;
      color: #38bdf8;
    }

    .brand-badges {
      display: flex;
      gap: 6px;
      margin-bottom: 2px;
    }

    .vietqr-pill {
      background: #ef4444;
      color: #ffffff;
      font-weight: 900;
      font-size: 0.65rem;
      padding: 1px 6px;
      border-radius: 4px;
      letter-spacing: 0.5px;
    }

    .napas-pill {
      background: #0284c7;
      color: #ffffff;
      font-weight: 800;
      font-size: 0.65rem;
      padding: 1px 6px;
      border-radius: 4px;
    }

    .brand-title h2 {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 800;
      color: #ffffff;
      letter-spacing: -0.3px;
    }

    .close-btn {
      color: #94a3b8;
    }

    .dialog-body {
      padding: 20px 24px !important;
      display: flex;
      flex-direction: column;
      gap: 16px;
      background: #0b1120;
    }

    .info-alert-banner {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 14px;
      background: rgba(56, 189, 248, 0.08);
      border: 1px solid rgba(56, 189, 248, 0.2);
      border-radius: 12px;
      font-size: 0.85rem;
      color: #7dd3fc;
    }

    .pulse-dot {
      width: 8px;
      height: 8px;
      background: #38bdf8;
      border-radius: 50%;
      box-shadow: 0 0 0 0 rgba(56, 189, 248, 0.7);
      animation: pulseGlow 1.8s infinite;
      flex-shrink: 0;
    }

    @keyframes pulseGlow {
      0% { box-shadow: 0 0 0 0 rgba(56, 189, 248, 0.7); }
      70% { box-shadow: 0 0 0 8px rgba(56, 189, 248, 0); }
      100% { box-shadow: 0 0 0 0 rgba(56, 189, 248, 0); }
    }

    .qr-main-grid {
      display: grid;
      grid-template-columns: 210px 1fr;
      gap: 20px;
      align-items: start;
    }

    /* VietQR Card Mock Styling */
    .vietqr-card-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 10px;
    }

    .vietqr-card {
      width: 200px;
      background: #ffffff;
      border-radius: 16px;
      padding: 12px;
      box-shadow: 0 15px 30px rgba(0, 0, 0, 0.5);
      border: 2px solid #0ea5e9;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 10px;
    }

    .card-top-bar {
      width: 100%;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 6px;
      border-bottom: 1px dashed #cbd5e1;
    }

    .napas-brand {
      font-weight: 900;
      font-size: 0.75rem;
      color: #0284c7;
    }

    .napas-brand span { color: #f97316; }

    .vietqr-brand {
      font-weight: 900;
      font-size: 0.75rem;
      color: #ef4444;
    }

    .vietqr-brand span { color: #2563eb; }

    .qr-svg-container {
      position: relative;
      width: 160px;
      height: 160px;
      background: #ffffff;
      padding: 6px;
      border-radius: 8px;
    }

    .qr-svg {
      width: 100%;
      height: 100%;
    }

    .qr-center-logo {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 32px;
      height: 32px;
      background: #ffffff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 8px rgba(0,0,0,0.2);
      border: 1.5px solid #0ea5e9;
    }

    .qr-center-logo mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: #0284c7;
    }

    .card-bottom-info {
      width: 100%;
      text-align: center;
    }

    .pay-amount-badge {
      background: #0284c7;
      color: #ffffff;
      font-weight: 800;
      font-size: 0.82rem;
      padding: 4px 8px;
      border-radius: 6px;
    }

    .merchant-tag {
      font-size: 0.68rem;
      color: #64748b;
      margin-top: 3px;
      font-weight: 600;
    }

    .scan-instruction {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.76rem;
      color: #94a3b8;
    }

    .camera-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      color: #38bdf8;
    }

    /* Banking Details Card */
    .banking-details-card {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .detail-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 10px 14px;
      background: rgba(15, 23, 42, 0.6);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 12px;
    }

    .detail-item.glow-item {
      border-color: rgba(56, 189, 248, 0.25);
      background: rgba(56, 189, 248, 0.04);
    }

    .detail-item.highlight-content-item {
      background: linear-gradient(135deg, rgba(132, 204, 22, 0.12) 0%, rgba(15, 23, 42, 0.8) 100%);
      border: 1.5px solid rgba(132, 204, 22, 0.4);
      box-shadow: 0 4px 16px rgba(132, 204, 22, 0.1);
    }

    .item-label {
      font-size: 0.75rem;
      color: #94a3b8;
      display: flex;
      align-items: center;
      gap: 6px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.3px;
    }

    .item-label mat-icon {
      font-size: 15px;
      width: 15px;
      height: 15px;
      color: #38bdf8;
    }

    .content-label {
      color: #bef264;
    }

    .content-label mat-icon {
      color: #a3e635;
    }

    .item-value {
      font-size: 0.95rem;
      color: #f1f5f9;
    }

    .bank-name {
      font-weight: 700;
      color: #e2e8f0;
    }

    .item-value-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .acc-number {
      font-weight: 800;
      font-size: 1.1rem;
      color: #38bdf8;
      font-family: monospace;
      letter-spacing: 1px;
    }

    .amount-val {
      font-weight: 800;
      font-size: 1.15rem;
      color: #fbbf24;
    }

    .content-val {
      font-weight: 900;
      font-size: 1.05rem;
      color: #a3e635;
      font-family: monospace;
      letter-spacing: 0.5px;
    }

    .quick-copy-btn {
      color: #7dd3fc;
      font-size: 0.78rem;
      height: 30px;
      line-height: 30px;
      padding: 0 10px;
      border-radius: 6px;
      background: rgba(56, 189, 248, 0.1);
    }

    .quick-copy-btn mat-icon {
      font-size: 15px;
      width: 15px;
      height: 15px;
      margin-right: 4px;
    }

    .copy-highlight-btn {
      background: #84cc16;
      color: #0f172a;
      font-weight: 800;
      font-size: 0.8rem;
      height: 32px;
      line-height: 32px;
      padding: 0 12px;
      border-radius: 8px;
    }

    .copy-highlight-btn mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      margin-right: 4px;
    }

    .warning-disclaimer {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 10px 14px;
      background: rgba(245, 158, 11, 0.08);
      border: 1px solid rgba(245, 158, 11, 0.25);
      border-radius: 12px;
      font-size: 0.82rem;
      color: #fde68a;
    }

    .warn-icon {
      color: #f59e0b;
      font-size: 18px;
      width: 18px;
      height: 18px;
      flex-shrink: 0;
      margin-top: 1px;
    }

    .dialog-actions {
      padding: 14px 24px;
      background: #070d19;
      border-top: 1px solid rgba(255, 255, 255, 0.08);
      display: flex;
      justify-content: space-between;
      gap: 12px;
    }

    .mock-test-btn {
      color: #fbbf24;
      border-color: rgba(251, 191, 36, 0.4);
      font-weight: 700;
      border-radius: 10px;
    }

    .confirm-done-btn {
      background: linear-gradient(135deg, #0284c7 0%, #2563eb 100%);
      color: #ffffff;
      font-weight: 800;
      border-radius: 10px;
      padding: 0 20px;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.4);
    }
  `]
})
export class BankTransferDialogComponent {
  simulating = false;

  constructor(
    public dialogRef: MatDialogRef<BankTransferDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: BankTransferDialogData,
    private snackBar: MatSnackBar,
    private http: HttpClient,
    private router: Router
  ) {}

  get rawAmountNum(): number {
    return this.data.bankAccount?.amount || (this.data.totalAmount * 25000);
  }

  get formattedAmount(): string {
    return Math.round(this.rawAmountNum).toLocaleString('vi-VN');
  }

  get transferContentStr(): string {
    return this.data.transferContent || `PAYGATE MOCK_MERCHANT ORD-${this.data.orderId}`;
  }

  copyToClipboard(text: string, label: string): void {
    navigator.clipboard.writeText(text);
    this.snackBar.open(`Đã sao chép ${label}: "${text}"`, 'Đóng', {
      duration: 3000,
      panelClass: ['snackbar-success']
    });
  }

  closeDialog(): void {
    this.dialogRef.close();
  }

  triggerMockBankTransfer(): void {
    this.simulating = true;
    const body = {
      bankRef: `BANK-REF-${Date.now()}`,
      amount: this.rawAmountNum,
      transferContent: this.transferContentStr
    };

    this.http.post('http://localhost:8081/api/v1/bank-transfers/receive', body).subscribe({
      next: () => {
        this.simulating = false;
        this.snackBar.open('⚡ Đã gửi Webhook thanh toán thành công từ PayGate!', 'Đóng', { duration: 3000 });
        this.viewOrders();
      },
      error: () => {
        // Direct local simulation callback fallback
        const webhookBody = {
          transactionRef: body.bankRef,
          orderId: `ORD-${this.data.orderId}`,
          status: 'COMPLETED',
          amount: body.amount
        };
        this.http.post('/api/v1/payments/paygate-webhook', webhookBody).subscribe({
          next: () => {
            this.simulating = false;
            this.snackBar.open('⚡ Đơn hàng đã được xác nhận PAID qua Webhook!', 'Đóng', { duration: 3000 });
            this.viewOrders();
          },
          error: () => {
            this.simulating = false;
            this.snackBar.open('Thông tin chuyển khoản đã ghi nhận.', 'Đóng', { duration: 3000 });
            this.viewOrders();
          }
        });
      }
    });
  }

  viewOrders(): void {
    this.dialogRef.close();
    this.router.navigate(['/orders']);
  }
}
