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

## Catalog metadata

`products.brand` is optional. `products.attributes` is a JSON object containing
at most 50 product-level key/value pairs. Sellable price continues to live only
on `product_variants`; the recommendation strategy does not move or duplicate
SKU pricing.
