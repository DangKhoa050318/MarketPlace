import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Delivery, DeliveryEventType } from '../../../core/models/delivery.model';

@Component({
  selector: 'app-delivery-timeline',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <section class="delivery-card" aria-labelledby="delivery-heading">
      <div class="delivery-header">
        <div>
          <h2 id="delivery-heading">Delivery tracking</h2>
          <p>{{ delivery.carrier }} · {{ delivery.trackingCode }}</p>
        </div>
        <span class="delivery-status" [ngClass]="'status-' + delivery.status.toLowerCase()">
          {{ delivery.status.replace('_', ' ') }}
        </span>
      </div>

      <div class="delivery-meta">
        <span><mat-icon>event</mat-icon>Estimated {{ delivery.estimatedDelivery | date:'mediumDate' }}</span>
        <span *ngIf="delivery.deliveredAt">
          <mat-icon>task_alt</mat-icon>Delivered {{ delivery.deliveredAt | date:'medium' }}
        </span>
      </div>

      <ol class="timeline" aria-label="Delivery journey">
        <li *ngFor="let event of delivery.events; let last = last" [class.last]="last">
          <div class="marker"><mat-icon>{{ iconFor(event.eventType) }}</mat-icon></div>
          <div class="event-content">
            <div class="event-heading">
              <strong>{{ labelFor(event.eventType) }}</strong>
              <time [attr.datetime]="event.occurredAt">{{ event.occurredAt | date:'medium' }}</time>
            </div>
            <p *ngIf="event.note">{{ event.note }}</p>
          </div>
        </li>
      </ol>
    </section>
  `,
  styles: [`
    .delivery-card { padding: 24px; border-radius: 16px; background: var(--surface-card, rgba(15, 23, 42, .78)); border: 1px solid var(--border-subtle, rgba(148, 163, 184, .2)); }
    .delivery-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
    h2 { margin: 0; font-size: 1.3rem; }
    .delivery-header p { margin: 5px 0 0; color: var(--text-muted); }
    .delivery-status { border-radius: 999px; padding: 6px 12px; font-size: .78rem; font-weight: 800; white-space: nowrap; }
    .status-pending { color: #a16207; background: #fef9c3; }
    .status-in_transit { color: #0369a1; background: #e0f2fe; }
    .status-delivered { color: #15803d; background: #dcfce7; }
    .status-failed { color: #b91c1c; background: #fee2e2; }
    .delivery-meta { display: flex; flex-wrap: wrap; gap: 18px; margin: 18px 0 22px; color: var(--text-muted); font-size: .9rem; }
    .delivery-meta span { display: inline-flex; align-items: center; gap: 6px; }
    .delivery-meta mat-icon { width: 18px; height: 18px; font-size: 18px; }
    .timeline { list-style: none; margin: 0; padding: 0; }
    .timeline li { position: relative; display: grid; grid-template-columns: 38px 1fr; gap: 12px; min-height: 68px; }
    .timeline li:not(.last)::after { content: ''; position: absolute; left: 17px; top: 34px; bottom: 0; width: 2px; background: rgba(14, 165, 233, .35); }
    .marker { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; color: #0284c7; background: #e0f2fe; z-index: 1; }
    .marker mat-icon { font-size: 20px; width: 20px; height: 20px; }
    .event-content { padding: 6px 0 18px; }
    .event-heading { display: flex; justify-content: space-between; gap: 16px; }
    time { color: var(--text-muted); font-size: .8rem; white-space: nowrap; }
    .event-content p { margin: 5px 0 0; color: var(--text-muted); }
    @media (max-width: 600px) { .delivery-header, .event-heading { flex-direction: column; } time { white-space: normal; } }
  `]
})
export class DeliveryTimelineComponent {
  @Input({ required: true }) delivery!: Delivery;

  labelFor(type: DeliveryEventType): string {
    const labels: Record<DeliveryEventType, string> = {
      DELIVERY_CREATED: 'Delivery created',
      READY_FOR_PICKUP: 'Ready for pickup',
      PICKED_UP: 'Picked up by carrier',
      IN_TRANSIT: 'In transit',
      OUT_FOR_DELIVERY: 'Out for delivery',
      DELIVERED: 'Delivered',
      DELIVERY_FAILED: 'Delivery failed'
    };
    return labels[type];
  }

  iconFor(type: DeliveryEventType): string {
    const icons: Record<DeliveryEventType, string> = {
      DELIVERY_CREATED: 'inventory_2',
      READY_FOR_PICKUP: 'package_2',
      PICKED_UP: 'local_shipping',
      IN_TRANSIT: 'route',
      OUT_FOR_DELIVERY: 'delivery_dining',
      DELIVERED: 'task_alt',
      DELIVERY_FAILED: 'error'
    };
    return icons[type];
  }
}
