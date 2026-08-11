import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { VietQrService } from '../../../core/services/vietqr.service';

export interface VietQrDialogData {
  orderId: string | number;
  amountVnd: number;
  description?: string;
  transferContent?: string;
  vietQrUrl?: string;
  bankBin?: string;
  bankName?: string;
  accountNo?: string;
  accountName?: string;
  qrPayload?: string;
  timerSeconds?: number;
}

@Component({
  selector: 'app-vietqr-dialog',
  standalone: true,
  imports: [
    CommonModule,
    CurrencyPipe,
    MatIconModule,
    MatTooltipModule
  ],
  template: `
    <div class="paygate-vqr-modal">
      <!-- Top Banner Header -->
      <div class="vqr-banner-header">
        <div class="vqr-title-group">
          <div class="vqr-badge-pill">
            <span class="dot-live"></span>
            <span>VIETQR NAPAS 24/7 GATEWAY</span>
          </div>
          <h3>Scan VietQR Code To Pay</h3>
        </div>
        <button type="button" class="btn-close-modal-light" (click)="onClose()" matTooltip="Close">✕</button>
      </div>

      <!-- Scrollable Content -->
      <div class="vqr-content-wrapper">
        <div class="modal-body-vqr">
          <!-- Left Column: QR Code Display -->
          <div class="qr-display-box">
            <!-- Image Frame -->
            <div class="qr-image-wrapper" [class.loading]="loadingQr">
              <img *ngIf="displayQrUrl" [src]="displayQrUrl" (error)="onQrImageError()" alt="Official VietQR Payment Code" class="vqr-img" />
              <div *ngIf="loadingQr" class="qr-loader">
                <span class="spinner-icon">⚡</span>
                <span>Generating official VietQR code...</span>
              </div>
            </div>

            <!-- Template Selector Pills -->
            <div class="tpl-selector-pills">
              <button type="button" [class.active]="selectedTemplate === 'compact2'" (click)="selectTemplate('compact2')">Card compact2</button>
              <button type="button" [class.active]="selectedTemplate === 'compact'" (click)="selectTemplate('compact')">Compact</button>
              <button type="button" [class.active]="selectedTemplate === 'qr_only'" (click)="selectTemplate('qr_only')">QR Only</button>
            </div>

            <!-- Timer Pill -->
            <div class="qr-timer-pill">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#c20067" stroke-width="2.2">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              <span>Code valid for: <strong>{{ formattedTimer }}</strong></span>
            </div>
          </div>

          <!-- Right Column: Transfer Info -->
          <div class="vqr-details-box">
            <!-- RECEIVING BANK -->
            <div class="detail-card">
              <span class="d-lbl">RECEIVING BANK</span>
              <div class="bank-head-val">
                <span class="bank-badge-ic">🏦</span>
                <span class="d-val font-bold">{{ bankNameDisplay }}</span>
              </div>
            </div>

            <!-- RECEIVING ACCOUNT NUMBER -->
            <div class="detail-card">
              <span class="d-lbl">RECEIVING ACCOUNT NUMBER</span>
              <div class="d-val-copy">
                <span class="font-mono acc-num">{{ accountNo }}</span>
                <button type="button" class="btn-copy-chip" (click)="copyText(accountNo, 'Account Number')">Copy</button>
              </div>
            </div>

            <!-- ACCOUNT HOLDER NAME -->
            <div class="detail-card">
              <span class="d-lbl">ACCOUNT HOLDER NAME</span>
              <span class="d-val font-bold text-uppercase">{{ accountName }}</span>
            </div>

            <!-- TRANSFER AMOUNT -->
            <div class="detail-card">
              <span class="d-lbl">TRANSFER AMOUNT</span>
              <span class="d-val amount-val">{{ amountVnd | currency:'VND':'symbol':'1.0-0' }}</span>
            </div>

            <!-- TRANSFER CONTENT -->
            <div class="detail-card highlight-note">
              <span class="d-lbl">TRANSFER CONTENT (MUST BE EXACT)</span>
              <div class="d-val-copy">
                <span class="font-mono text-note">{{ transferContent }}</span>
                <button type="button" class="btn-copy-chip btn-copy-highlight" (click)="copyText(transferContent, 'Transfer Content')">Copy</button>
              </div>
            </div>

            <!-- EMVCO VIETQR STRING -->
            <div class="emvco-box" *ngIf="emvCoPayload">
              <div class="emvco-head">
                <span class="emvco-lbl">EMVCO VIETQR STRING</span>
                <button type="button" class="btn-copy-chip" (click)="copyText(emvCoPayload, 'EMVCo QR String')">Copy QR String</button>
              </div>
              <code class="emvco-string">{{ emvCoPayload }}</code>
            </div>

            <!-- Toast Notice -->
            <div class="copy-toast-banner" *ngIf="copyNotice">
              <span>✓ {{ copyNotice }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Bottom Action Footer -->
      <div class="modal-footer-vqr">
        <button type="button" class="btn-cancel-modal" (click)="onClose()">Close</button>
        <div class="vqr-right-actions">
          <button type="button" class="btn-mock-cancel-qr" (click)="onCancelPayment()" matTooltip="Cancel order and release reserved stock">
            ✕ Cancel Payment
          </button>
          <button type="button" class="btn-confirm-vqr" (click)="onConfirmPaid()">
            ✓ I Have Transferred
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    ::ng-deep .paygate-vqr-dialog-panel .mat-mdc-dialog-container .mdc-dialog__surface {
      padding: 0 !important;
      border-radius: 24px !important;
      overflow: hidden !important;
      max-width: 840px !important;
      background: #ffffff !important;
    }

    :host {
      display: block;
      width: 100%;
    }

    .paygate-vqr-modal {
      background: #ffffff;
      border-radius: 24px;
      width: 100%;
      max-width: 840px;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      box-shadow: 0 25px 70px rgba(0, 0, 0, 0.35);
      font-family: 'Inter', system-ui, -apple-system, sans-serif;
    }

    /* Header Banner */
    .vqr-banner-header {
      background: linear-gradient(135deg, #c20067 0%, #a00055 40%, #0072ce 100%);
      padding: 20px 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      color: #ffffff;
      flex-shrink: 0;
    }

    .vqr-badge-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 0.72rem;
      font-weight: 800;
      color: #f8bbd0;
      background: rgba(255, 255, 255, 0.15);
      border: 1px solid rgba(255, 255, 255, 0.25);
      padding: 3px 12px;
      border-radius: 20px;
      letter-spacing: 0.05em;
      margin-bottom: 4px;
    }

    .dot-live {
      width: 7px;
      height: 7px;
      background: #10b981;
      border-radius: 50%;
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.4);
    }

    .vqr-title-group h3 {
      margin: 0;
      font-size: 1.4rem;
      font-weight: 900;
      color: #ffffff;
      letter-spacing: -0.02em;
    }

    .btn-close-modal-light {
      background: rgba(255, 255, 255, 0.15);
      border: 1px solid rgba(255, 255, 255, 0.25);
      border-radius: 50%;
      width: 36px;
      height: 36px;
      font-size: 16px;
      color: #ffffff;
      cursor: pointer;
      font-weight: 800;
      transition: all 0.15s;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .btn-close-modal-light:hover {
      background: rgba(255, 255, 255, 0.3);
    }

    /* Content Area */
    .vqr-content-wrapper {
      padding: 24px 32px;
      display: flex;
      flex-direction: column;
      gap: 20px;
      overflow-y: auto;
      max-height: 75vh;
    }

    .modal-body-vqr {
      display: grid;
      grid-template-columns: 280px 1fr;
      gap: 28px;
      align-items: start;
    }

    /* Left Column */
    .qr-display-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 14px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 20px;
      padding: 20px 16px;
    }

    .qr-template-selector {
      display: flex;
      gap: 4px;
      background: #f3d6e5;
      padding: 4px;
      border-radius: 12px;
      width: 100%;
    }

    .btn-tpl {
      flex: 1;
      border: none;
      background: transparent;
      padding: 6px 0;
      font-size: 0.75rem;
      font-weight: 800;
      color: #475569;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s;
    }

    .btn-tpl.active {
      background: #ffffff;
      color: #c20067;
      box-shadow: 0 2px 6px rgba(194, 0, 103, 0.15);
    }

    .qr-image-wrapper {
      width: 220px;
      height: 220px;
      background: #ffffff;
      padding: 10px;
      border-radius: 20px;
      border: 3px solid #f8bbd0;
      box-shadow: 0 10px 24px rgba(194, 0, 103, 0.1);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .vqr-img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }

    .qr-loader {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      color: #c20067;
      font-size: 0.85rem;
      font-weight: 700;
    }

    .spinner-icon {
      font-size: 24px;
      animation: pulse 1s infinite alternate;
    }

    @keyframes pulse {
      from { transform: scale(1); opacity: 0.7; }
      to { transform: scale(1.2); opacity: 1; }
    }

    .qr-timer-pill {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.82rem;
      color: #c20067;
      background: #fff0f6;
      padding: 6px 16px;
      border-radius: 20px;
      border: 1px solid #f8bbd0;
      font-weight: 700;
    }

    /* Right Column Details */
    .vqr-details-box {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .detail-card {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 10px 16px;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .detail-card.highlight-note {
      background: #f0fdf4;
      border-color: #bbf7d0;
    }

    .d-lbl {
      font-size: 0.68rem;
      font-weight: 800;
      color: #64748b;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .d-val {
      font-size: 0.95rem;
      color: #0f172a;
    }

    .bank-head-val {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .bank-badge-ic {
      font-size: 1.1rem;
    }

    .amount-val {
      font-size: 1.4rem;
      font-weight: 900;
      color: #c20067;
    }

    .acc-num {
      font-size: 1.2rem;
      font-weight: 800;
      color: #1e293b;
      letter-spacing: 0.04em;
    }

    .d-val-copy {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
    }

    .text-note {
      color: #c20067;
      font-weight: 900;
      font-size: 1rem;
      word-break: break-all;
    }

    .btn-copy-chip {
      background: #ffffff;
      border: 1px solid #c20067;
      border-radius: 8px;
      padding: 4px 12px;
      font-size: 0.78rem;
      font-weight: 800;
      color: #c20067;
      cursor: pointer;
      transition: all 0.15s;
      flex-shrink: 0;
    }
    .btn-copy-chip:hover {
      background: #c20067;
      color: #ffffff;
    }

    .btn-copy-highlight {
      background: #c20067;
      color: #ffffff;
    }
    .btn-copy-highlight:hover {
      background: #a00055;
    }

    /* EMVCo Payload Box */
    .emvco-box {
      background: #fffafc;
      border: 1px solid #f3d6e5;
      border-radius: 12px;
      padding: 10px 14px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .emvco-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .emvco-lbl {
      font-size: 0.68rem;
      font-weight: 800;
      color: #94a3b8;
    }

    .emvco-string {
      font-size: 0.7rem;
      font-family: ui-monospace, monospace;
      color: #0f172a;
      word-break: break-all;
      max-height: 48px;
      overflow-y: auto;
      background: #ffffff;
      padding: 6px 10px;
      border-radius: 8px;
      border: 1px solid #f3d6e5;
    }

    .copy-toast-banner {
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
      color: #065f46;
      padding: 8px 14px;
      border-radius: 10px;
      font-size: 0.82rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      gap: 6px;
      animation: fadeIn 0.2s ease-in;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-4px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .tpl-selector-pills {
      display: flex;
      gap: 6px;
      margin-top: 8px;
      justify-content: center;
    }
    .tpl-selector-pills button {
      background: #f1f5f9;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      padding: 4px 8px;
      font-size: 0.72rem;
      font-weight: 700;
      color: #64748b;
      cursor: pointer;
      transition: all 0.15s;
    }
    .tpl-selector-pills button.active {
      background: #c20067;
      color: #ffffff;
      border-color: #c20067;
    }

    /* Footer */
    .modal-footer-vqr {
      padding: 16px 28px;
      background: #ffffff;
      border-top: 1px solid #f3d6e5;
      display: grid;
      grid-template-columns: 1fr 2.2fr;
      gap: 14px;
      flex-shrink: 0;
    }

    .btn-cancel-modal {
      background: #f1f5f9;
      border: 1px solid #cbd5e1;
      border-radius: 14px;
      height: 46px;
      font-size: 0.9rem;
      font-weight: 800;
      color: #475569;
      cursor: pointer;
      transition: background 0.15s;
    }
    .btn-cancel-modal:hover {
      background: #e2e8f0;
    }

    .vqr-right-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      width: 100%;
    }
    .btn-mock-cancel-qr {
      background: #fef2f2;
      border: 1px solid #fca5a5;
      border-radius: 14px;
      height: 46px;
      padding: 0 16px;
      font-size: 0.9rem;
      font-weight: 800;
      color: #dc2626;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.15s ease;
    }
    .btn-mock-cancel-qr:hover {
      background: #fee2e2;
      border-color: #ef4444;
      transform: translateY(-1px);
    }
    .btn-confirm-vqr {
      flex: 1;
      background: linear-gradient(135deg, #c20067 0%, #0072ce 100%);
      border: none;
      border-radius: 14px;
      height: 46px;
      font-size: 0.95rem;
      font-weight: 900;
      color: #ffffff;
      cursor: pointer;
      box-shadow: 0 4px 16px rgba(194, 0, 103, 0.35);
      transition: transform 0.15s, box-shadow 0.15s;
    }
    .btn-confirm-vqr:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(194, 0, 103, 0.45);
    }

    @media (max-width: 680px) {
      .modal-body-vqr {
        grid-template-columns: 1fr;
        gap: 16px;
      }
      .qr-image-wrapper {
        width: 180px;
        height: 180px;
      }
      .modal-footer-vqr {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class VietQrDialogComponent implements OnInit, OnDestroy {
  selectedBankBin = '970422'; // MBBank
  accountNo = '8888999988';
  accountName = 'PAYGATE GATEWAY SYSTEM';
  amountVnd = 0;
  transferContent = '';
  emvCoPayload = '';

  selectedTemplate: 'compact2' | 'compact' | 'qr_only' = 'compact2';
  qrDataUrl: string | null = null;
  loadingQr = true;
  copyNotice: string | null = null;

  timerSeconds = 900; // 15 minutes (900 seconds)
  timerInterval: ReturnType<typeof setInterval> | null = null;

  get bankNameDisplay(): string {
    if (this.data.bankName) {
      return this.data.bankName;
    }
    const b = this.vietQrService.findBankByBin(this.selectedBankBin);
    return b ? `${b.shortName} - ${b.name}` : 'MBBank - Ngân hàng TMCP Quân Đội';
  }

  get formattedTimer(): string {
    if (this.timerSeconds <= 0) {
      return '00:00 (Expired)';
    }
    const mins = Math.floor(this.timerSeconds / 60);
    const secs = this.timerSeconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  constructor(
    private vietQrService: VietQrService,
    public dialogRef: MatDialogRef<VietQrDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: VietQrDialogData
  ) {}

  ngOnInit(): void {
    if (this.data.timerSeconds != null && this.data.timerSeconds > 0) {
      this.timerSeconds = this.data.timerSeconds;
    }
    if (this.data.bankBin) {
      this.selectedBankBin = this.data.bankBin;
    }
    if (this.data.accountNo) {
      this.accountNo = this.data.accountNo;
    }
    if (this.data.accountName) {
      this.accountName = this.data.accountName;
    }
    this.amountVnd = this.data.amountVnd || 0;

    const rawId = String(this.data.orderId || '').replace(/^ORD-/, '');
    if (this.data.transferContent) {
      this.transferContent = this.data.transferContent;
    } else if (this.data.description && this.data.description.startsWith('PAYGATE ')) {
      this.transferContent = this.data.description;
    } else {
      this.transferContent = `PAYGATE ORD-${rawId}`;
    }

    if (this.data.qrPayload) {
      this.emvCoPayload = this.data.qrPayload;
    } else {
      this.emvCoPayload = this.vietQrService.generateQrContent({
        bankId: this.selectedBankBin,
        accountId: this.accountNo,
        accountName: this.accountName,
        amount: this.amountVnd,
        description: this.transferContent
      });
    }

    this.renderQrCode();
    this.startTimer();
  }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  startTimer(): void {
    this.stopTimer();
    this.timerInterval = setInterval(() => {
      if (this.timerSeconds > 0) {
        this.timerSeconds--;
      } else {
        this.stopTimer();
      }
    }, 1000);
  }

  stopTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  vietQrQuickLinkUrl: string | null = null;
  useFallback = false;

  get displayQrUrl(): string | null {
    if (this.useFallback) {
      return this.qrDataUrl;
    }
    return this.vietQrQuickLinkUrl || this.qrDataUrl;
  }

  onQrImageError(): void {
    console.warn('VietQR QuickLink image failed to load, switching to raw EMVCo QR code canvas');
    this.useFallback = true;
  }

  selectTemplate(tpl: 'compact2' | 'compact' | 'qr_only'): void {
    this.selectedTemplate = tpl;
    this.renderQrCode();
  }

  async renderQrCode(): Promise<void> {
    this.loadingQr = true;
    this.useFallback = false;
    try {
      if (this.data.vietQrUrl && this.selectedTemplate === 'compact2') {
        this.vietQrQuickLinkUrl = this.data.vietQrUrl;
      } else {
        const bin = this.selectedBankBin || '970422';
        const acc = this.accountNo || 'SYS0000000000000001';
        const amt = this.amountVnd || 0;
        const addInfo = encodeURIComponent(this.transferContent || '');
        const accName = encodeURIComponent(this.accountName || '');
        this.vietQrQuickLinkUrl = `https://img.vietqr.io/image/${bin}-${acc}-${this.selectedTemplate}.png?amount=${amt}&addInfo=${addInfo}&accountName=${accName}`;
      }

      if (this.emvCoPayload) {
        this.qrDataUrl = await this.vietQrService.renderRawQrDataUrl(this.emvCoPayload);
      } else {
        this.qrDataUrl = await this.vietQrService.generateQrDataUrl({
          bankId: this.selectedBankBin,
          accountId: this.accountNo,
          accountName: this.accountName,
          amount: this.amountVnd,
          description: this.transferContent
        });
      }
    } catch (err) {
      console.error('Error generating VietQR Data URL:', err);
    } finally {
      this.loadingQr = false;
    }
  }

  copyText(text: string, label: string): void {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).then(() => {
        this.showNotice(`Copied ${label}!`);
      });
    }
  }

  showNotice(msg: string): void {
    this.copyNotice = msg;
    setTimeout(() => {
      this.copyNotice = null;
    }, 3000);
  }

  onClose(): void {
    this.dialogRef.close(false);
  }

  onCancelPayment(): void {
    this.dialogRef.close('CANCEL');
  }

  onConfirmPaid(): void {
    this.dialogRef.close(true);
  }
}
