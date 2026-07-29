# Best-Seller Recommendation

This document describes the rule-based `BEST_SELLER` strategy delivered by
`REQ-STP-B-506`.

## Ranking

Sales are aggregated from `order_items` through their `variantId` to the owning
product/SPU. Results are ordered by:

1. Distinct valid order count, descending.
2. Units sold, descending.
3. Product ID, ascending, for deterministic ties.

Counting distinct orders prevents a product with multiple SKU lines in one order
from inflating its order count. Units sold remain a secondary signal.

The query only includes active products, active variants, orders created inside
the configured lookback window, and the configured valid lifecycle statuses. An
optional `RecommendationContext.categoryId` restricts the result to one category.
No-sales windows return an empty list; fallback behavior belongs to a later
recommendation orchestration requirement.

## Configuration

| Property | Environment variable | Default |
| --- | --- | --- |
| `marketplace.recommendation.best-seller-lookback` | `RECOMMENDATION_BEST_SELLER_LOOKBACK` | `30d` |
| `marketplace.recommendation.valid-order-statuses` | `RECOMMENDATION_VALID_ORDER_STATUSES` | `CONFIRMED,PROCESSING,SHIPPED,DELIVERED` |

The lookback must be at least one day. `PENDING` and `CANCELLED` cannot be
configured as valid sales; invalid configuration fails application startup.
