import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CouponPreviewResponse } from '../models/promotion.model';
import { PromotionService } from './promotion.service';

describe('PromotionService applied coupon state', () => {
  let service: PromotionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(PromotionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores a valid normalized coupon for the shared cart state', () => {
    const states: Array<CouponPreviewResponse | null> = [];
    service.appliedCoupon$.subscribe(value => states.push(value));

    service.apply(' school10 ').subscribe();

    const request = http.expectOne(req => req.url.endsWith('/coupons/preview'));
    expect(request.request.body).toEqual({ code: 'SCHOOL10' });
    request.flush(apiResponse(validPreview()));
    const applied = states[states.length - 1];
    expect(applied?.code).toBe('SCHOOL10');
    expect(applied?.discountAmount).toBe(1800000);
  });

  it('clears the selected coupon when a revalidation becomes invalid', () => {
    const states: Array<CouponPreviewResponse | null> = [];
    service.appliedCoupon$.subscribe(value => states.push(value));

    service.apply('SCHOOL10').subscribe();
    http.expectOne(req => req.url.endsWith('/coupons/preview')).flush(apiResponse(validPreview()));

    service.revalidateApplied().subscribe();
    http.expectOne(req => req.url.endsWith('/coupons/preview')).flush(apiResponse({
      ...validPreview(),
      valid: false,
      reason: 'MIN_ORDER_NOT_MET',
      discountAmount: 0,
      newTotal: 18000000
    }));

    expect(states[states.length - 1]).toBeNull();
  });
});

function validPreview(): CouponPreviewResponse {
  return {
    code: 'SCHOOL10',
    valid: true,
    discountType: 'PERCENT',
    eligibleSubtotal: 18000000,
    discountAmount: 1800000,
    cartSubtotal: 18000000,
    newTotal: 16200000
  };
}

function apiResponse(data: CouponPreviewResponse) {
  return {
    success: true,
    message: 'OK',
    data,
    timestamp: new Date().toISOString()
  };
}
