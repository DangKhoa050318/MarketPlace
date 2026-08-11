# Similar Product Recommendation

This document describes the rule-based `SIMILAR` strategy delivered by
`REQ-STP-B-505`.

## Inputs

The strategy requires `RecommendationContext.productId` and uses product-level
category, brand, and attributes plus the minimum and maximum prices of active
SKUs. Brand and attribute comparisons are case-insensitive and trim surrounding
whitespace.

## Scoring

| Signal | Weight | Rule |
| --- | ---: | --- |
| Category | 35% | Exact category ID match |
| Brand | 25% | Exact normalized non-blank brand match |
| Attributes | 25% | Jaccard similarity of normalized key/value pairs |
| Price range | 15% | Full score for overlapping active-SKU ranges; otherwise decreases with the gap |

A candidate must match category, brand, or at least one attribute. Price
proximity alone cannot make unrelated products similar.

Results are ordered by descending score, then ascending product ID for
deterministic ties. The source product and inactive products are excluded. The
strategy returns internal `RecommendationCandidate` values; API response
hydration and the complete eligibility pipeline remain separate concerns.

## Serving and precomputation

`SIMILAR` does not load the active catalog into Java. Results are served from a
Redis cache first and then from the indexed `product_similarities` Top-K table.
If a source has not been computed yet, one bounded PostgreSQL query builds a
candidate pool from indexed category, normalized brand, and normalized
attribute terms, applies the scoring formula above, and persists the best
results. A PostgreSQL advisory transaction lock prevents concurrent cold
requests from recomputing the same source.

The default limits are configurable:

- `marketplace.recommendation.similar-candidate-pool-size=500` per signal.
- `marketplace.recommendation.similar-precompute-size=50` stored results per source.

Catalog mutations publish an after-commit event. Sources whose current or new
recommendations may be affected are marked stale, the Redis similarity cache is
cleared, and the changed product is warmed asynchronously. This keeps catalog
writes independent from recommendation computation while preventing stale
results from being served.

## Catalog metadata

`products.brand` is optional. `products.attributes` is a JSON object containing
at most 50 product-level key/value pairs. Sellable price continues to live only
on `product_variants`; the recommendation strategy does not move or duplicate
SKU pricing.

`product_attribute_terms` is an inverted index maintained by a database trigger.
Each entry uses the same normalized `key=value` representation as the scoring
rule, allowing attribute-overlap candidate discovery without scanning JSON for
every product during a request.
