# Recommendation Eligibility Filter

This document describes the final candidate filtering boundary delivered by
`REQ-STP-B-508`.

## Contract

Recommendation consumers resolve algorithms through
`RecommendationStrategyRegistry`. The registry decorates every registered
strategy with `RecommendationCandidateFilter`, so final filtering happens after
candidate selection and ranking but before results leave the strategy boundary.
Concrete strategy beans must not be called directly by controllers or
application orchestration.

The filter:

1. Removes the source product when the context contains a source product ID.
2. Removes duplicate product IDs while preserving the first, highest-ranked
   occurrence.
3. Keeps only active products with at least one active variant.
4. Preserves the remaining candidates' strategy order, score, and reason.
5. Applies the requested limit after eligibility filtering.

Eligibility is loaded in one batch repository query to avoid per-candidate
database access.

## Catalog lifecycle mapping

The current catalog model uses `products.active = false` for products hidden or
discontinued from the storefront. A product with no active
`product_variants` is also not sellable and is excluded. This implementation
therefore requires no new column, API contract, or Flyway migration.

If the catalog later introduces separate visibility or lifecycle fields, their
public-display conditions must be added to
`ProductRepository.findRecommendationEligibleProductIds` so all recommendation
strategies continue to share one eligibility rule.
