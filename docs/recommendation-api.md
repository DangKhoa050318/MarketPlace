# Recommendation API

The public storefront endpoint exposes the rule-based strategies implemented by
`REQ-STP-B-504` through `REQ-STP-B-508`.

## Endpoint

```http
GET /api/v1/recommendations
```

The endpoint is public. An authenticated user is derived from JWT; anonymous
identity may be supplied through `X-Session-Id`. A user ID is never accepted as
a request parameter.

| Parameter | Required | Rules |
| --- | --- | --- |
| `placement` | Yes | One of the supported placements below |
| `productId` | Product-detail placements | Positive source product/SPU ID |
| `categoryId` | `CATEGORY_BEST_SELLERS` | Positive category ID |
| `limit` | No | Default `12`; range `1`–`24` |

### Placement mapping

| Placement | Strategy | Required context |
| --- | --- | --- |
| `PRODUCT_DETAIL_SIMILAR` | `SIMILAR` | `productId` |
| `PRODUCT_DETAIL_CO_VIEWED` | `CO_VIEWED` | `productId` |
| `PRODUCT_DETAIL_CO_PURCHASED` | `CO_PURCHASED` | `productId` |
| `HOME_BEST_SELLERS` | `BEST_SELLER` | None |
| `CATEGORY_BEST_SELLERS` | `BEST_SELLER` | `categoryId` |

Example:

```http
GET /api/v1/recommendations?placement=PRODUCT_DETAIL_SIMILAR&productId=123&limit=12
X-Session-Id: 0ed9a567-a76f-4f47-b6c4-e98ae274c464
```

## Response

```json
{
  "success": true,
  "data": {
    "requestId": "93fc3727-47ae-4cbe-88ee-f9934753deca",
    "placement": "PRODUCT_DETAIL_SIMILAR",
    "strategy": "SIMILAR",
    "generatedAt": "2026-07-29T03:00:00Z",
    "items": [
      {
        "position": 0,
        "product": {
          "id": 456,
          "slug": "example-product",
          "name": "Example product",
          "active": true,
          "variants": [
            {
              "id": 789,
              "productId": 456,
              "sku": "EXAMPLE-001",
              "price": 499.99,
              "active": true
            }
          ]
        },
        "score": 0.85,
        "reason": "same_category,same_brand"
      }
    ]
  }
}
```

Only active variants are returned. Product hydration is performed in batch and
rechecks storefront eligibility to protect against catalog status changes
between ranking and response mapping. An empty recommendation is a successful
response with `items: []`.

## Analytics correlation

The client must reuse the response values when sending
`RECOMMENDATION_IMPRESSION` or `RECOMMENDATION_CLICK`:

- `recommendationRequestId` = response `requestId`
- `placement` = response `placement`
- `strategy` = response `strategy`
- `position` = item `position`
- `productId` = item `product.id`
- `source` = `RECOMMENDATION`

Each analytics event still requires its own unique `eventId`.
