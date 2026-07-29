import { convertToParamMap } from '@angular/router';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { AnalyticsEventType } from '../../../core/models/analytics-event.model';
import { ProductDetailComponent } from './product-detail.component';

describe('ProductDetailComponent', () => {
  it('ignores responses from the previous product after route reuse', () => {
    const routeParams = new BehaviorSubject(convertToParamMap({ id: '10' }));
    const productResponses = new Map<number, Subject<unknown>>([
      [10, new Subject()],
      [20, new Subject()]
    ]);
    const productService = {
      getProductById: jasmine.createSpy('getProductById').and.callFake(
        (productId: number) => productResponses.get(productId)
      ),
      getVariants: jasmine.createSpy('getVariants').and.returnValue(of({ data: [] }))
    };
    const reviewService = {
      getSummary: jasmine.createSpy('getSummary').and.returnValue(of({ data: undefined })),
      getReviews: jasmine.createSpy('getReviews').and.returnValue(of({
        data: { content: [], totalElements: 0 }
      }))
    };
    const analyticsService = {
      track: jasmine.createSpy('track')
    };
    const attributionService = {
      consumeProductViewContext: jasmine.createSpy('consumeProductViewContext'),
      clear: jasmine.createSpy('clear'),
      contextFor: jasmine.createSpy('contextFor')
    };
    const component = new ProductDetailComponent(
      { paramMap: routeParams.asObservable() } as any,
      {} as any,
      productService as any,
      reviewService as any,
      { isAuthenticated: () => false } as any,
      {} as any,
      {} as any,
      analyticsService as any,
      attributionService as any,
      { record: () => of({}) } as any,
      { status: () => of({ data: { wishlisted: false } }) } as any
    );

    component.ngOnInit();
    routeParams.next(convertToParamMap({ id: '20' }));
    productResponses.get(20)?.next({
      data: { id: 20, name: 'Product B', categoryId: 2, categoryName: 'B' }
    });
    productResponses.get(10)?.next({
      data: { id: 10, name: 'Product A', categoryId: 1, categoryName: 'A' }
    });

    expect(component.product?.id).toBe(20);
    expect(analyticsService.track).toHaveBeenCalledTimes(1);
    expect(analyticsService.track).toHaveBeenCalledWith(
      AnalyticsEventType.ProductView,
      jasmine.objectContaining({ productName: 'Product B' }),
      jasmine.objectContaining({ productId: 20 })
    );

    component.ngOnDestroy();
  });
});
