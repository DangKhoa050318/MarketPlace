import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import {
  AnalyticsEventSource,
  AnalyticsEventType,
  RecommendationPlacement,
  RecommendationStrategyType
} from '../../../core/models/analytics-event.model';
import { RecommendationResponse } from '../../../core/models/recommendation.model';
import { AnalyticsService } from '../../../core/services/analytics.service';
import {
  RecommendationAttributionService
} from '../../../core/services/recommendation-attribution.service';
import { RecommendationService } from '../../../core/services/recommendation.service';
import { RecommendationCarouselComponent } from './recommendation-carousel.component';

describe('RecommendationCarouselComponent', () => {
  let fixture: ComponentFixture<RecommendationCarouselComponent>;
  let component: RecommendationCarouselComponent;
  let recommendationService: jasmine.SpyObj<RecommendationService>;
  let analyticsService: jasmine.SpyObj<AnalyticsService>;
  let attributionService: jasmine.SpyObj<RecommendationAttributionService>;
  let intersectionCallback: IntersectionObserverCallback;
  let intersectionOptions: IntersectionObserverInit | undefined;
  let observeSpy: jasmine.Spy;
  let unobserveSpy: jasmine.Spy;
  let disconnectSpy: jasmine.Spy;
  let originalIntersectionObserver: typeof IntersectionObserver;

  const response: RecommendationResponse = {
    requestId: '93fc3727-47ae-4cbe-88ee-f9934753deca',
    placement: RecommendationPlacement.ProductDetailSimilar,
    strategy: RecommendationStrategyType.Similar,
    generatedAt: '2026-07-29T03:00:00Z',
    items: [
      {
        position: 0,
        score: 0.91,
        reason: 'same_category,same_brand',
        product: {
          id: 21,
          slug: 'recommended-product',
          name: 'Recommended Product',
          categoryId: 3,
          categoryName: 'Computers',
          brand: 'Example',
          active: true,
          imageUrl: 'https://example.test/product.jpg',
          variants: [{
            id: 210,
            productId: 21,
            sku: 'REC-210',
            variantName: 'Standard',
            price: 499,
            active: true
          }]
        }
      }
    ]
  };

  beforeEach(async () => {
    originalIntersectionObserver = window.IntersectionObserver;
    observeSpy = jasmine.createSpy('observe');
    unobserveSpy = jasmine.createSpy('unobserve');
    disconnectSpy = jasmine.createSpy('disconnect');
    class MockIntersectionObserver implements IntersectionObserver {
      readonly root = null;
      readonly rootMargin = '0px';
      readonly thresholds = [0.5];

      constructor(
        callback: IntersectionObserverCallback,
        options?: IntersectionObserverInit
      ) {
        intersectionCallback = callback;
        intersectionOptions = options;
      }

      observe = observeSpy;
      unobserve = unobserveSpy;
      disconnect = disconnectSpy;
      takeRecords(): IntersectionObserverEntry[] {
        return [];
      }
    }
    Object.defineProperty(window, 'IntersectionObserver', {
      configurable: true,
      writable: true,
      value: MockIntersectionObserver
    });

    recommendationService = jasmine.createSpyObj<RecommendationService>(
      'RecommendationService',
      ['getRecommendations']
    );
    analyticsService = jasmine.createSpyObj<AnalyticsService>('AnalyticsService', ['track']);
    attributionService = jasmine.createSpyObj<RecommendationAttributionService>(
      'RecommendationAttributionService',
      ['remember', 'contextFor']
    );
    recommendationService.getRecommendations.and.returnValue(of({
      success: true,
      message: 'Success',
      data: response,
      timestamp: '2026-07-29T03:00:00Z'
    }));

    await TestBed.configureTestingModule({
      imports: [RecommendationCarouselComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: RecommendationService, useValue: recommendationService },
        { provide: AnalyticsService, useValue: analyticsService },
        { provide: RecommendationAttributionService, useValue: attributionService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RecommendationCarouselComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('title', 'Sản phẩm tương tự');
    fixture.componentRef.setInput('placement', RecommendationPlacement.ProductDetailSimilar);
    fixture.componentRef.setInput('productId', 10);
    fixture.detectChanges();
  });

  afterEach(() => {
    Object.defineProperty(window, 'IntersectionObserver', {
      configurable: true,
      writable: true,
      value: originalIntersectionObserver
    });
  });

  it('loads and renders an accessible recommendation region', () => {
    expect(recommendationService.getRecommendations).toHaveBeenCalledWith({
      placement: RecommendationPlacement.ProductDetailSimilar,
      productId: 10,
      categoryId: undefined,
      limit: 12
    });

    const region = fixture.nativeElement.querySelector('[role="region"]') as HTMLElement;
    const heading = fixture.nativeElement.querySelector('h2') as HTMLElement;
    const card = fixture.nativeElement.querySelector('.recommendation-card') as HTMLElement;
    const controls = fixture.nativeElement.querySelector('.controls') as HTMLElement;
    const viewport = fixture.nativeElement.querySelector('.carousel') as HTMLElement;
    expect(region.getAttribute('aria-labelledby')).toBe(heading.id);
    expect(card.getAttribute('aria-label')).toContain('Recommended Product');
    expect(controls.getAttribute('role')).toBe('group');
    expect(viewport.getAttribute('aria-roledescription')).toBe('carousel');
    expect(fixture.nativeElement.textContent).toContain('Sản phẩm tương tự');
    expect(fixture.nativeElement.textContent).toContain('₫499');
  });

  it('tracks an impression once when a card reaches the 50 percent threshold', () => {
    const card = fixture.nativeElement.querySelector('.recommendation-card') as HTMLElement;
    const observer = {
      observe: observeSpy,
      unobserve: unobserveSpy,
      disconnect: disconnectSpy
    } as unknown as IntersectionObserver;
    const entry = {
      target: card,
      isIntersecting: true,
      intersectionRatio: 0.49,
      boundingClientRect: card.getBoundingClientRect(),
      intersectionRect: card.getBoundingClientRect(),
      rootBounds: null,
      time: 0
    } as IntersectionObserverEntry;

    expect(intersectionOptions).toEqual({ threshold: 0.5 });
    expect(observeSpy).toHaveBeenCalledWith(card);

    intersectionCallback([entry], observer);
    expect(analyticsService.track).not.toHaveBeenCalled();

    const visibleEntry = { ...entry, intersectionRatio: 0.5 };
    intersectionCallback([visibleEntry], observer);
    intersectionCallback([visibleEntry], observer);

    expect(analyticsService.track).toHaveBeenCalledTimes(1);
    expect(unobserveSpy).toHaveBeenCalledWith(card);
    expect(analyticsService.track).toHaveBeenCalledWith(
      AnalyticsEventType.RecommendationImpression,
      { score: 0.91, reason: 'same_category,same_brand' },
      {
        productId: 21,
        source: AnalyticsEventSource.Recommendation,
        placement: RecommendationPlacement.ProductDetailSimilar,
        recommendationRequestId: response.requestId,
        strategy: RecommendationStrategyType.Similar,
        position: 0
      }
    );
  });

  it('disconnects the previous impression observer when recommendations reload', () => {
    disconnectSpy.calls.reset();

    component.load();

    expect(disconnectSpy).toHaveBeenCalled();
  });

  it('tracks clicks and remembers attribution for downstream product events', () => {
    component.trackClick(response.items[0]);

    expect(analyticsService.track).toHaveBeenCalledWith(
      AnalyticsEventType.RecommendationClick,
      { score: 0.91, reason: 'same_category,same_brand' },
      jasmine.objectContaining({
        productId: 21,
        source: AnalyticsEventSource.Recommendation,
        recommendationRequestId: response.requestId,
        position: 0
      })
    );
    expect(attributionService.remember).toHaveBeenCalledWith({
      productId: 21,
      recommendationRequestId: response.requestId,
      placement: RecommendationPlacement.ProductDetailSimilar,
      strategy: RecommendationStrategyType.Similar,
      position: 0
    });
  });

  it('hides the complete block when the API returns no recommendations', () => {
    recommendationService.getRecommendations.and.returnValue(of({
      success: true,
      message: 'Success',
      data: { ...response, items: [] },
      timestamp: '2026-07-29T03:00:00Z'
    }));

    component.load();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.recommendation-block')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('chưa có sản phẩm gợi ý');
  });

  it('supports keyboard navigation on the carousel viewport', () => {
    const scrollSpy = spyOn(component, 'scroll');
    const viewport = fixture.nativeElement.querySelector('.carousel') as HTMLElement;

    viewport.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }));

    expect(scrollSpy).toHaveBeenCalledWith(1);
  });
});
