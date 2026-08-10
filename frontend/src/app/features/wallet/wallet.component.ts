import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { UserService, User } from '../users/user.service';

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, MatButtonModule],
  template: `
    <section class="wallet-page">
      <header class="wallet-header">
        <div>
          <p class="eyebrow">Marketplace Wallet</p>
          <h1>Wallet balance</h1>
          <p class="subtext">Credit from cancelled BNPL orders is kept here and can be used with Marketplace Wallet at checkout.</p>
        </div>
        <a mat-stroked-button routerLink="/products">
          <mat-icon>storefront</mat-icon>
          Shop
        </a>
      </header>

      <div class="balance-panel">
        <div class="balance-icon">
          <mat-icon>account_balance_wallet</mat-icon>
        </div>
        <div>
          <span class="balance-label">Available</span>
          <strong>{{ walletBalance | currency:'USD':'symbol':'1.2-2' }}</strong>
        </div>
      </div>

      <div class="wallet-note">
        <mat-icon>info</mat-icon>
        <span>When a paid BNPL Marketplace order is cancelled, the order amount is added here instead of being refunded through PayGate.</span>
      </div>
    </section>
  `,
  styles: [`
    .wallet-page { max-width: 880px; margin: 0 auto; padding: 28px 0 56px; display: grid; gap: 20px; }
    .wallet-header { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; }
    .eyebrow { margin: 0 0 6px; color: #5b21b6; font-weight: 800; font-size: .78rem; text-transform: uppercase; }
    h1 { margin: 0; color: #0f172a; font-size: 2rem; font-weight: 900; }
    .subtext { margin: 8px 0 0; color: #64748b; max-width: 620px; line-height: 1.55; }
    .balance-panel { display: flex; align-items: center; gap: 18px; padding: 26px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; box-shadow: 0 10px 30px rgba(15,23,42,.08); }
    .balance-icon { width: 56px; height: 56px; border-radius: 8px; display: grid; place-items: center; color: #5b21b6; background: #f3e8ff; }
    .balance-icon mat-icon { font-size: 30px; width: 30px; height: 30px; }
    .balance-label { display: block; color: #64748b; font-weight: 700; font-size: .85rem; margin-bottom: 4px; }
    strong { font-size: 2.3rem; line-height: 1; color: #111827; }
    .wallet-note { display: flex; gap: 10px; align-items: flex-start; padding: 14px 16px; border-radius: 8px; background: #f8fafc; color: #475569; border: 1px solid #e2e8f0; }
    .wallet-note mat-icon { color: #0284c7; }
  `]
})
export class WalletComponent implements OnInit {
  walletBalance = 0;

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.userService.getMe().subscribe({
      next: response => this.walletBalance = Number((response.data as User | undefined)?.walletBalance ?? 0),
      error: () => this.walletBalance = 0
    });
  }
}
