import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { BannerResponse } from '../../../core/models/banner.model';
import { MerchandisingService } from '../../../core/services/merchandising.service';
import { MerchandisingImpressionDirective } from '../../../shared/directives/merchandising-impression.directive';

/**
 * F-404/F-405: renders the currently effective banner for a slot (server decides visibility) and
 * fires a BANNER impression once it is 50% visible, plus a BANNER click before navigation.
 */
@Component({
  selector: 'app-merchandising-banner',
  standalone: true,
  imports: [CommonModule, MerchandisingImpressionDirective],
  template: `
    @if (banner) {
      <a
        class="hero-banner"
        appMerchandisingImpression
        (impressed)="onImpression()"
        [href]="banner.targetUrl || null"
        [attr.target]="isExternal ? '_blank' : null"
        [attr.rel]="isExternal ? 'noopener noreferrer' : null"
        (click)="onClick()">
        <picture>
          @if (banner.imageUrlMobile) {
            <source media="(max-width: 640px)" [srcset]="banner.imageUrlMobile" />
          }
          <img [src]="banner.imageUrlDesktop" [alt]="banner.altText" loading="lazy" />
        </picture>
        @if (banner.title) {
          <span class="caption">{{ banner.title }}</span>
        }
      </a>
    }
  `,
  styles: [`
    :host { display: block; }
    .hero-banner {
      position: relative;
      display: block;
      overflow: hidden;
      border-radius: 18px;
      box-shadow: 0 12px 30px rgba(15, 23, 42, 0.12);
    }
    .hero-banner img {
      width: 100%;
      height: auto;
      max-height: 380px;
      object-fit: cover;
      display: block;
    }
    .caption {
      position: absolute;
      left: 18px;
      bottom: 16px;
      padding: 8px 14px;
      border-radius: 10px;
      background: rgba(15, 23, 42, 0.6);
      color: #fff;
      font-weight: 800;
      font-size: 1.05rem;
      backdrop-filter: blur(4px);
    }
  `]
})
export class MerchandisingBannerComponent implements OnInit {
  @Input() position = 'HOME_HERO';

  banner?: BannerResponse;

  constructor(private merchandising: MerchandisingService) {}

  ngOnInit(): void {
    this.merchandising.effectiveBanners(this.position).subscribe({
      next: res => (this.banner = res.success ? (res.data ?? [])[0] : undefined),
      error: () => (this.banner = undefined)
    });
  }

  get isExternal(): boolean {
    return !!this.banner?.targetUrl && /^https?:\/\//i.test(this.banner.targetUrl);
  }

  onImpression(): void {
    if (this.banner) {
      this.merchandising.recordEvent('IMPRESSION', 'BANNER', this.banner.id);
    }
  }

  onClick(): void {
    if (this.banner) {
      this.merchandising.recordEvent('CLICK', 'BANNER', this.banner.id);
    }
  }
}
