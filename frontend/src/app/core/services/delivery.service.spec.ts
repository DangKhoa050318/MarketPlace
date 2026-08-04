import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DeliveryService } from './delivery.service';
import { environment } from '../../../environments/environment';
import { Delivery } from '../models/delivery.model';

describe('DeliveryService', () => {
  let service: DeliveryService;
  let http: HttpTestingController;

  const delivery: Delivery = {
    id: 20,
    orderId: 10,
    version: 0,
    carrier: 'GHN',
    trackingCode: 'GHN-001',
    status: 'PENDING',
    estimatedDelivery: '2026-08-07',
    events: [],
    createdAt: '2026-08-04T09:00:00',
    updatedAt: '2026-08-04T09:00:00'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(DeliveryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads customer delivery from the owned order endpoint', () => {
    service.getCustomerDelivery(10).subscribe(response => {
      expect(response.data).toEqual(delivery);
    });

    const request = http.expectOne(`${environment.apiUrl}/orders/10/delivery`);
    expect(request.request.method).toBe('GET');
    request.flush({ success: true, data: delivery });
  });

  it('creates delivery through the admin order endpoint', () => {
    const payload = {
      carrier: 'GHN',
      trackingCode: 'GHN-001',
      estimatedDelivery: '2026-08-07'
    };

    service.create(10, payload).subscribe(response => {
      expect(response.data?.id).toBe(20);
    });

    const request = http.expectOne(`${environment.apiUrl}/admin/orders/10/delivery`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ success: true, data: delivery });
  });

  it('uses PATCH when transitioning delivery status', () => {
    service.updateStatus(20, {
      expectedVersion: 0,
      status: 'IN_TRANSIT',
      eventType: 'PICKED_UP'
    }).subscribe();

    const request = http.expectOne(`${environment.apiUrl}/admin/deliveries/20/status`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({
      expectedVersion: 0,
      status: 'IN_TRANSIT',
      eventType: 'PICKED_UP'
    });
    request.flush({ success: true, data: { ...delivery, status: 'IN_TRANSIT' } });
  });
});
