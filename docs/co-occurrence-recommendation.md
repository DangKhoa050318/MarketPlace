# Co-Occurrence Recommendations

This document describes the rule-based `CO_VIEWED` and `CO_PURCHASED`
strategies delivered by `REQ-STP-B-507`.

## Frequently viewed together

`CO_VIEWED` uses `PRODUCT_VIEW` analytics events inside the configured lookback
window. A viewer is identified by a non-blank session ID when available, with
the authenticated user ID as fallback. Repeated views of the same product by
the same viewer count once, so refreshes do not inflate the relationship.

Candidate products are ranked by the number of distinct viewers who viewed both
the source product and the candidate:

1. Co-viewer count, descending.
2. Product ID, ascending, for deterministic ties.

Only active candidate products with at least one active variant are included.
The source product is excluded, and a candidate must meet the configured
minimum co-view occurrence threshold.

## Frequently purchased together

`CO_PURCHASED` aggregates order items from SKU/variant level to product/SPU
level. Two products co-occur when they appear in the same valid order inside
the configured lookback window. Each order counts once even when it contains
multiple lines or variants of the same product.

Candidates are ranked by:

1. Distinct valid order count, descending.
2. Product ID, ascending, for deterministic ties.

The strategy uses the same valid order statuses as `BEST_SELLER`. Only active
candidate products with at least one active variant are included. The source
product is excluded, and a candidate must meet the configured minimum
co-purchase occurrence threshold.

Both strategies validate that the source product exists and is active. If no
candidate reaches the threshold, they return an empty list; API composition and
fallback behavior belong to later recommendation requirements.

## Configuration

| Property | Environment variable | Default |
| --- | --- | --- |
| `marketplace.recommendation.co-viewed-lookback` | `RECOMMENDATION_CO_VIEWED_LOOKBACK` | `90d` |
| `marketplace.recommendation.min-co-viewed-occurrences` | `RECOMMENDATION_MIN_CO_VIEWED_OCCURRENCES` | `2` |
| `marketplace.recommendation.co-purchased-lookback` | `RECOMMENDATION_CO_PURCHASED_LOOKBACK` | `90d` |
| `marketplace.recommendation.min-co-purchased-occurrences` | `RECOMMENDATION_MIN_CO_PURCHASED_OCCURRENCES` | `2` |

Both lookback windows must be at least one day, and both occurrence thresholds
must be at least two. Invalid configuration fails application startup.
