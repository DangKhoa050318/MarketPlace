import { Injectable } from '@angular/core';
import {
  AnalyticsEventContext,
  AnalyticsEventSource,
  RecommendationPlacement,
  RecommendationStrategyType
} from '../models/analytics-event.model';

export interface RecommendationAttribution {
  productId: number;
  recommendationRequestId: string;
  placement: RecommendationPlacement;
  strategy: RecommendationStrategyType;
  position: number;
  recordedAt: number;
  productViewConsumed: boolean;
}

@Injectable({ providedIn: 'root' })
export class RecommendationAttributionService {
  private readonly storagePrefix = 'recommendation_attribution:';
  private readonly maxAgeMs = 30 * 60 * 1000;

  remember(
    attribution: Omit<RecommendationAttribution, 'recordedAt' | 'productViewConsumed'>
  ): void {
    if (typeof sessionStorage === 'undefined') return;
    sessionStorage.setItem(
      this.storageKey(attribution.productId),
      JSON.stringify({
        ...attribution,
        recordedAt: Date.now(),
        productViewConsumed: false
      })
    );
  }

  contextFor(productId: number): AnalyticsEventContext | undefined {
    const attribution = this.read(productId);
    return attribution ? this.toContext(attribution) : undefined;
  }

  consumeProductViewContext(productId: number): AnalyticsEventContext | undefined {
    const attribution = this.read(productId);
    if (!attribution || attribution.productViewConsumed) return undefined;

    const consumedAttribution = {
      ...attribution,
      productViewConsumed: true
    };
    sessionStorage.setItem(
      this.storageKey(productId),
      JSON.stringify(consumedAttribution)
    );
    return this.toContext(consumedAttribution);
  }

  clear(productId: number): void {
    if (typeof sessionStorage === 'undefined') return;
    sessionStorage.removeItem(this.storageKey(productId));
  }

  private read(productId: number): RecommendationAttribution | undefined {
    if (typeof sessionStorage === 'undefined') return undefined;
    const key = this.storageKey(productId);
    const stored = sessionStorage.getItem(key);
    if (!stored) return undefined;

    try {
      const attribution: unknown = JSON.parse(stored);
      if (!this.isValid(attribution, productId)) {
        sessionStorage.removeItem(key);
        return undefined;
      }
      return attribution;
    } catch {
      sessionStorage.removeItem(key);
      return undefined;
    }
  }

  private isValid(
    value: unknown,
    productId: number
  ): value is RecommendationAttribution {
    if (!value || typeof value !== 'object') return false;
    const attribution = value as Record<string, unknown>;
    const recordedAt = attribution['recordedAt'];
    const age = typeof recordedAt === 'number' ? Date.now() - recordedAt : Number.NaN;

    return Number.isInteger(productId)
      && productId > 0
      && attribution['productId'] === productId
      && typeof attribution['recommendationRequestId'] === 'string'
      && attribution['recommendationRequestId'].trim().length > 0
      && Object.values(RecommendationPlacement).includes(
        attribution['placement'] as RecommendationPlacement
      )
      && Object.values(RecommendationStrategyType).includes(
        attribution['strategy'] as RecommendationStrategyType
      )
      && typeof attribution['position'] === 'number'
      && Number.isInteger(attribution['position'])
      && attribution['position'] >= 0
      && Number.isFinite(age)
      && age >= 0
      && age <= this.maxAgeMs
      && typeof attribution['productViewConsumed'] === 'boolean';
  }

  private toContext(attribution: RecommendationAttribution): AnalyticsEventContext {
    return {
      productId: attribution.productId,
      source: AnalyticsEventSource.Recommendation,
      placement: attribution.placement,
      recommendationRequestId: attribution.recommendationRequestId,
      strategy: attribution.strategy,
      position: attribution.position
    };
  }

  private storageKey(productId: number): string {
    return `${this.storagePrefix}${productId}`;
  }
}
