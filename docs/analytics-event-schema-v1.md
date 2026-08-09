# Analytics Event Schema v1

This document is the canonical contract for `REQ-STP-B-501` through
`REQ-STP-B-503`, including persistence, type-specific validation, and
event-ID deduplication.

## Common envelope

All browser events use the following versioned envelope:

| Field | Type | Notes |
| --- | --- | --- |
| `eventId` | UUID | Generated once by the client and reused when retrying the same event. |
| `schemaVersion` | integer | Current value: `1`. |
| `type` | enum | Canonical event type. |
| `occurredAt` | ISO-8601 instant | UTC timestamp with an offset, normally `Z`. |
| `productId` | long | Product/SPU context. |
| `variantId` | long | Sellable SKU; required for cart and purchase events. |
| `source` | enum | Coarse-grained customer journey source. |
| `placement` | enum | Recommendation placement, when applicable. |
| `recommendationRequestId` | UUID | Correlates impression, click, and downstream actions. |
| `strategy` | enum | Rule-based strategy that produced the item. |
| `position` | integer | Zero-based item position in a recommendation placement. |
| `quantity` | integer | Positive SKU quantity for cart or purchase events. |
| `orderId` | long | Trusted order reference for server-generated purchase events. |
| `unitPrice` | decimal | Trusted order-item snapshot price; never accepted as authoritative from a browser. |
| `properties` | object | Optional non-canonical metadata. It must not contain PII. |

User identity is derived from the authenticated principal. Anonymous session
identity is transported through `X-Session-Id`; neither is accepted from the
request body.

## Event field matrix

`R` means required by the event contract, `C` means conditionally required, and
`-` means not applicable.

| Event | productId | variantId | source | placement/requestId/strategy/position | quantity | orderId/unitPrice |
| --- | --- | --- | --- | --- | --- | --- |
| `PRODUCT_VIEW` | R | optional | R | C: required when source is `RECOMMENDATION` | - | - |
| `RECOMMENDATION_IMPRESSION` | R | optional | `RECOMMENDATION` | R | - | - |
| `RECOMMENDATION_CLICK` | R | optional | `RECOMMENDATION` | R | - | - |
| `ADD_TO_CART` | R | R | R | C: required when source is `RECOMMENDATION` | R | - |
| `PURCHASE` | R | R | `ORDER_SERVICE` | optional attribution context | R | R |
| `CHAT_MESSAGE` | optional | optional | `CHAT_ASSISTANT` | - | - | - |
| `CHAT_PRODUCT_IMPRESSION` | R | optional | `CHAT_ASSISTANT` | - | - | - |
| `CHAT_PRODUCT_CLICK` | R | optional | `CHAT_ASSISTANT` | - | - | - |
| `CHAT_VOUCHER_CLICK` | R | optional | `CHAT_ASSISTANT` | - | - | - |

## Purchase trust boundary

`PURCHASE` is emitted by the backend from a trusted order lifecycle transition,
not accepted as an authoritative browser event. One event is emitted per order
item so co-purchase aggregation remains product/variant aware. Multiple purchase
events from one order share the same `orderId`.

## Ingestion validation and idempotency

- `eventId`, `type`, and `occurredAt` are required at the HTTP boundary.
- Only schema version `1` is accepted.
- Product-linked events require an existing active product. When a variant is
  supplied, it must exist, be active, and belong to that product.
- `ADD_TO_CART` requires a variant and a positive quantity.
- Recommendation events require source `RECOMMENDATION`, placement, request ID,
  strategy, and a zero-based position. Strategy must match the placement.
- Timestamps may be at most five minutes in the future and seven days old.
- `eventId` is unique. A retry with an existing ID does not overwrite the stored
  event and returns status `DUPLICATE_IGNORED`.

## Versioning

- Additive optional fields remain in schema version 1.
- Removing or changing the meaning/type of a field requires a new schema version.
- Enum values are stable identifiers and must not be reused with new semantics.
- Consumers must ignore unknown optional fields; ingestion rejects unsupported
  schema versions.
