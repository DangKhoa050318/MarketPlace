import {
  AnalyticsEventSource,
  RecommendationPlacement,
  RecommendationStrategyType
} from '../models/analytics-event.model';
import { RecommendationAttributionService } from './recommendation-attribution.service';

describe('RecommendationAttributionService', () => {
  let service: RecommendationAttributionService;
  const productId = 21;
  const storageKey = `recommendation_attribution:${productId}`;
  const attribution = {
    productId,
    recommendationRequestId: '93fc3727-47ae-4cbe-88ee-f9934753deca',
    placement: RecommendationPlacement.ProductDetailSimilar,
    strategy: RecommendationStrategyType.Similar,
    position: 0
  };

  beforeEach(() => {
    sessionStorage.clear();
    service = new RecommendationAttributionService();
  });

  afterEach(() => sessionStorage.clear());

  it('consumes product-view attribution once while retaining journey context', () => {
    service.remember(attribution);

    expect(service.consumeProductViewContext(productId)).toEqual({
      productId,
      source: AnalyticsEventSource.Recommendation,
      placement: RecommendationPlacement.ProductDetailSimilar,
      recommendationRequestId: attribution.recommendationRequestId,
      strategy: RecommendationStrategyType.Similar,
      position: 0
    });
    expect(service.consumeProductViewContext(productId)).toBeUndefined();
    expect(service.contextFor(productId)?.source).toBe(AnalyticsEventSource.Recommendation);
  });

  it('rejects malformed stored attribution and removes it', () => {
    sessionStorage.setItem(storageKey, JSON.stringify({
      ...attribution,
      placement: 'NOT_A_PLACEMENT',
      recordedAt: Date.now(),
      productViewConsumed: false
    }));

    expect(service.contextFor(productId)).toBeUndefined();
    expect(sessionStorage.getItem(storageKey)).toBeNull();
  });

  it('clears attribution for a direct product journey', () => {
    service.remember(attribution);

    service.clear(productId);

    expect(service.contextFor(productId)).toBeUndefined();
  });
});
