import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AnalyticsEventType } from '../models/analytics-event.model';
import { AnalyticsService } from './analytics.service';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(AnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('does not send non-essential events before consent', () => {
    service.track(AnalyticsEventType.ProductView, { productId: 10 });

    httpMock.expectNone(req => req.url.includes('/analytics/events'));
  });

  it('sends non-essential events after consent', () => {
    service.grantConsent();

    service.track(AnalyticsEventType.ProductView, { productId: 10 });

    httpMock.expectOne(req => req.url.includes('/analytics/events')).flush({ success: true, data: {} });
  });

  it('stops all tracking after opt-out', () => {
    service.grantConsent();
    service.optOut();

    service.track(AnalyticsEventType.AddToCart, { productId: 10 });

    httpMock.expectNone(req => req.url.includes('/analytics/events'));
  });
});
