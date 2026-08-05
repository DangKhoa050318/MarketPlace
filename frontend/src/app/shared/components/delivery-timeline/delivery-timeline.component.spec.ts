import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DeliveryTimelineComponent } from './delivery-timeline.component';
import { Delivery } from '../../../core/models/delivery.model';

describe('DeliveryTimelineComponent', () => {
  let fixture: ComponentFixture<DeliveryTimelineComponent>;

  const delivery: Delivery = {
    id: 20,
    orderId: 10,
    version: 1,
    carrier: 'GHN',
    trackingCode: 'GHN-001',
    status: 'IN_TRANSIT',
    estimatedDelivery: '2026-08-07',
    events: [
      {
        id: 1,
        requestId: '13b8619b-d6d3-4044-a9b1-793c5bcf7c27',
        eventType: 'DELIVERY_CREATED',
        status: 'PENDING',
        occurredAt: '2026-08-04T09:00:00'
      },
      {
        id: 2,
        requestId: 'f70e4fc2-7a1a-478d-90e0-465f5c68b071',
        eventType: 'PICKED_UP',
        status: 'IN_TRANSIT',
        note: 'Carrier received the parcel',
        occurredAt: '2026-08-04T10:00:00'
      }
    ],
    createdAt: '2026-08-04T09:00:00',
    updatedAt: '2026-08-04T10:00:00'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [DeliveryTimelineComponent] }).compileComponents();
    fixture = TestBed.createComponent(DeliveryTimelineComponent);
    fixture.componentInstance.delivery = delivery;
    fixture.detectChanges();
  });

  it('renders carrier, tracking code and ordered milestones', () => {
    const text = fixture.nativeElement.textContent;
    const items = fixture.nativeElement.querySelectorAll('.timeline li');

    expect(text).toContain('GHN · GHN-001');
    expect(text).toContain('Picked up by carrier');
    expect(text).toContain('Carrier received the parcel');
    expect(items.length).toBe(2);
  });
});
