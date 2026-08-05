import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MerchandisingService } from './merchandising.service';
import { environment } from '../../../environments/environment';

describe('MerchandisingService', () => {
  let service: MerchandisingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MerchandisingService]
    });
    service = TestBed.inject(MerchandisingService);
    http = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('records an impression with a generated eventId and a session id', () => {
    service.recordEvent('IMPRESSION', 'BANNER', 7);

    const req = http.expectOne(`${environment.apiUrl}/merchandising/events`);
    expect(req.request.method).toBe('POST');
    const body = req.request.body;
    expect(body.eventType).toBe('IMPRESSION');
    expect(body.targetType).toBe('BANNER');
    expect(body.targetId).toBe(7);
    expect(typeof body.eventId).toBe('string');
    expect(body.eventId.length).toBeGreaterThan(0);
    expect(typeof body.sessionId).toBe('string');
    req.flush({ success: true, message: 'Event recorded', data: null });
  });

  it('reuses the same session id across events', () => {
    service.recordEvent('CLICK', 'CAMPAIGN', 1);
    const first = http.expectOne(`${environment.apiUrl}/merchandising/events`);
    const sessionId = first.request.body.sessionId;
    first.flush({ success: true, message: 'ok', data: null });

    service.recordEvent('CLICK', 'CAMPAIGN', 2);
    const second = http.expectOne(`${environment.apiUrl}/merchandising/events`);
    expect(second.request.body.sessionId).toBe(sessionId);
    second.flush({ success: true, message: 'ok', data: null });
  });

  it('requests effective banners for a slot', () => {
    service.effectiveBanners('HOME_HERO').subscribe();

    const req = http.expectOne(r => r.url === `${environment.apiUrl}/banners`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('position')).toBe('HOME_HERO');
    req.flush({ success: true, message: 'ok', data: [] });
  });

  it('requests the effectiveness summary with target and range params', () => {
    service.summary('CAMPAIGN', 5, '2026-07-01T00:00', '2026-08-01T00:00').subscribe();

    const req = http.expectOne(r => r.url === `${environment.apiUrl}/admin/merchandising/summary`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('targetType')).toBe('CAMPAIGN');
    expect(req.request.params.get('targetId')).toBe('5');
    expect(req.request.params.get('from')).toBe('2026-07-01T00:00');
    expect(req.request.params.get('to')).toBe('2026-08-01T00:00');
    req.flush({ success: true, message: 'ok', data: null });
  });
});
