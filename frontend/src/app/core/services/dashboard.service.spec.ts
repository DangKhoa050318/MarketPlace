import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('requests analytics overview with the selected date range', () => {
    const from = new Date('2026-07-01T00:00:00.000Z');
    const to = new Date('2026-07-31T23:59:59.999Z');

    service.getAnalyticsOverview(from, to).subscribe();

    const request = httpMock.expectOne(req =>
      req.url === `${environment.apiUrl}/admin/analytics/overview`);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('from')).toBe(from.toISOString());
    expect(request.request.params.get('to')).toBe(to.toISOString());
    request.flush({ success: true, data: null });
  });
});
