import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { CampaignResponse } from '../../../core/models/campaign.model';
import { MerchandisingService } from '../../../core/services/merchandising.service';
import { MerchandisingImpressionDirective } from '../../../shared/directives/merchandising-impression.directive';

/**
 * F-404/F-405: shows the currently effective campaigns and *surfaces* their linked coupon code
 * (D-1: a campaign announces a coupon, it does not auto-apply a discount). Fires a CAMPAIGN
 * impression once each chip is 50% visible; clicking copies the coupon and records a CAMPAIGN click.
 */
@Component({
  selector: 'app-campaign-strip',
  standalone: true,
  imports: [CommonModule, MatIconModule, MerchandisingImpressionDirective],
  template: `
    @if (campaigns.length > 0) {
      <section class="strip" aria-label="Chương trình khuyến mãi đang diễn ra">
        @for (c of campaigns; track c.id) {
          <button
            type="button"
            class="chip"
            appMerchandisingImpression
            (impressed)="onImpression(c)"
            (click)="onClick(c)"
            [attr.aria-label]="c.couponCode ? ('Sao chép mã ' + c.couponCode + ' cho ' + c.name) : c.name">
            <mat-icon>campaign</mat-icon>
            <span class="body">
              <strong>{{ c.name }}</strong>
              @if (c.description) { <small>{{ c.description }}</small> }
            </span>
            @if (c.couponCode) {
              <span class="code">
                {{ copiedId === c.id ? 'Đã chép!' : c.couponCode }}
                <mat-icon>{{ copiedId === c.id ? 'check' : 'content_copy' }}</mat-icon>
              </span>
            }
          </button>
        }
      </section>
    }
  `,
  styles: [`
    :host { display: block; }
    .strip {
      display: flex;
      gap: 12px;
      margin: 20px 0;
      overflow-x: auto;
      padding-bottom: 6px;
    }
    .chip {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 260px;
      padding: 12px 14px;
      border: 1px solid #fde68a;
      border-radius: 14px;
      background: linear-gradient(135deg, #fffbeb, #fff7ed);
      color: #92400e;
      cursor: pointer;
      text-align: left;
      transition: transform .15s ease, box-shadow .15s ease;
    }
    .chip:hover { transform: translateY(-2px); box-shadow: 0 10px 20px rgba(180, 83, 9, .12); }
    .chip > mat-icon { color: #d97706; }
    .body { display: flex; flex-direction: column; min-width: 0; flex: 1; }
    .body strong { color: #78350f; font-size: .95rem; font-weight: 800; }
    .body small {
      color: #b45309;
      font-size: .74rem;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .code {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 5px 10px;
      border: 1px dashed #d97706;
      border-radius: 8px;
      background: #fff;
      color: #b45309;
      font-weight: 800;
      font-size: .8rem;
      letter-spacing: .04em;
      white-space: nowrap;
    }
    .code mat-icon { width: 15px; height: 15px; font-size: 15px; }
    @media (prefers-reduced-motion: reduce) { .chip { transition: none; } }
  `]
})
export class CampaignStripComponent implements OnInit {
  campaigns: CampaignResponse[] = [];
  copiedId?: number;

  constructor(private merchandising: MerchandisingService) {}

  ngOnInit(): void {
    this.merchandising.effectiveCampaigns().subscribe({
      next: res => (this.campaigns = res.success ? res.data ?? [] : []),
      error: () => (this.campaigns = [])
    });
  }

  onImpression(campaign: CampaignResponse): void {
    this.merchandising.recordEvent('IMPRESSION', 'CAMPAIGN', campaign.id);
  }

  onClick(campaign: CampaignResponse): void {
    this.merchandising.recordEvent('CLICK', 'CAMPAIGN', campaign.id);
    if (campaign.couponCode && typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(campaign.couponCode).then(
        () => {
          this.copiedId = campaign.id;
          setTimeout(() => (this.copiedId = undefined), 2000);
        },
        () => undefined
      );
    }
  }
}
