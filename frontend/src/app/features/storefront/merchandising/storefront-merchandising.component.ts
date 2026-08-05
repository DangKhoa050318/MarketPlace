import { Component } from '@angular/core';
import { CampaignStripComponent } from './campaign-strip.component';
import { CollectionShowcaseComponent } from './collection-showcase.component';
import { MerchandisingBannerComponent } from './merchandising-banner.component';

/**
 * F-404: single storefront entry point that composes the hero banner, the active-campaign strip
 * and a featured collection. Each child renders only when the backend returns effective content,
 * so dropping this on the home page is safe even when nothing is scheduled.
 */
@Component({
  selector: 'app-storefront-merchandising',
  standalone: true,
  imports: [MerchandisingBannerComponent, CampaignStripComponent, CollectionShowcaseComponent],
  template: `
    <app-merchandising-banner position="HOME_HERO"></app-merchandising-banner>
    <app-campaign-strip></app-campaign-strip>
    <app-collection-showcase></app-collection-showcase>
  `,
  styles: [':host { display: block; }']
})
export class StorefrontMerchandisingComponent {}
