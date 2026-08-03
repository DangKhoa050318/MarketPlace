import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { CollectionResponse } from '../../../core/models/collection.model';
import { MerchandisingService } from '../../../core/services/merchandising.service';
import { MerchandisingImpressionDirective } from '../../../shared/directives/merchandising-impression.directive';

/**
 * F-404/F-405: renders a published collection's curated products in the admin-defined order and
 * fires a COLLECTION impression once the section is 50% visible, plus a COLLECTION click before
 * navigating to a product. With no {@code slug} input it shows the first published collection.
 */
@Component({
  selector: 'app-collection-showcase',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, MerchandisingImpressionDirective],
  template: `
    @if (collection && collection.items.length > 0) {
      <section
        class="collection"
        appMerchandisingImpression
        (impressed)="onImpression()"
        [attr.aria-label]="collection.name">
        <header>
          <h2>{{ collection.name }}</h2>
          @if (collection.description) {
            <p>{{ collection.description }}</p>
          }
        </header>
        <div class="row">
          @for (item of collection.items; track item.productId) {
            <a
              class="card"
              [routerLink]="['/products', item.productId]"
              (click)="onClick()"
              [attr.aria-label]="item.productName || ('Product ' + item.productId)">
              <div class="img">
                <img
                  [src]="item.imageUrl || fallback"
                  [alt]="item.productName || 'Product'"
                  (error)="useFallback($event)"
                  loading="lazy" />
              </div>
              <span class="name">{{ item.productName || ('#' + item.productId) }}</span>
              <span class="cta">Xem <mat-icon>arrow_forward</mat-icon></span>
            </a>
          }
        </div>
      </section>
    }
  `,
  styles: [`
    :host { display: block; }
    .collection {
      margin: 24px 0;
      padding: 22px;
      border: 1px solid #e2e8f0;
      border-radius: 18px;
      background: #fff;
      box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
    }
    header h2 { margin: 0; font-size: clamp(1.2rem, 2.2vw, 1.55rem); font-weight: 850; color: #0f172a; }
    header p { margin: 4px 0 0; color: #64748b; font-size: .9rem; }
    .row {
      display: grid;
      grid-auto-flow: column;
      grid-auto-columns: minmax(180px, 22%);
      gap: 14px;
      margin-top: 16px;
      overflow-x: auto;
      scroll-snap-type: inline mandatory;
      padding-bottom: 8px;
    }
    .card {
      min-width: 0;
      overflow: hidden;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      background: #fff;
      color: inherit;
      text-decoration: none;
      scroll-snap-align: start;
      transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;
    }
    .card:hover, .card:focus-visible {
      border-color: #0e7490;
      outline: none;
      transform: translateY(-3px);
      box-shadow: 0 12px 24px rgba(14, 116, 144, .14);
    }
    .img { aspect-ratio: 4 / 3; overflow: hidden; background: #f1f5f9; }
    .img img { width: 100%; height: 100%; object-fit: cover; }
    .name {
      display: -webkit-box;
      min-height: 2.7em;
      overflow: hidden;
      margin: 0;
      padding: 11px 13px 0;
      color: #0f172a;
      font-size: .92rem;
      font-weight: 800;
      line-height: 1.35;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
    }
    .cta {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      padding: 6px 13px 13px;
      color: #0369a1;
      font-size: .74rem;
      font-weight: 750;
    }
    .cta mat-icon { width: 15px; height: 15px; font-size: 15px; }
    @media (max-width: 980px) { .row { grid-auto-columns: minmax(170px, 42%); } }
    @media (max-width: 620px) { .row { grid-auto-columns: minmax(160px, 78%); } }
    @media (prefers-reduced-motion: reduce) { .card { transition: none; } }
  `]
})
export class CollectionShowcaseComponent implements OnInit {
  @Input() slug?: string;

  collection?: CollectionResponse;
  readonly fallback = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800';

  constructor(private merchandising: MerchandisingService) {}

  ngOnInit(): void {
    if (this.slug) {
      this.loadBySlug(this.slug);
      return;
    }
    this.merchandising.publishedCollections().subscribe({
      next: res => {
        const first = (res.success ? res.data ?? [] : [])[0];
        if (first) {
          this.loadBySlug(first.slug);
        }
      },
      error: () => (this.collection = undefined)
    });
  }

  onImpression(): void {
    if (this.collection) {
      this.merchandising.recordEvent('IMPRESSION', 'COLLECTION', this.collection.id);
    }
  }

  onClick(): void {
    if (this.collection) {
      this.merchandising.recordEvent('CLICK', 'COLLECTION', this.collection.id);
    }
  }

  useFallback(event: Event): void {
    (event.target as HTMLImageElement).src = this.fallback;
  }

  private loadBySlug(slug: string): void {
    this.merchandising.collectionBySlug(slug).subscribe({
      next: res => (this.collection = res.success ? res.data : undefined),
      error: () => (this.collection = undefined)
    });
  }
}
