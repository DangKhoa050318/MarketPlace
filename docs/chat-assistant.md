# Shopping Chat Assistant

The shopping assistant is a Spring Boot orchestration layer that combines Gemini intent extraction
with authoritative Marketplace data. Gemini never queries the database and never supplies product,
price, stock, campaign, or voucher facts.

## Endpoint

```http
POST /api/v1/chat/messages
X-Session-Id: <required for anonymous visitors>
Authorization: Bearer <optional>
Content-Type: application/json
```

```json
{
  "conversationId": null,
  "message": "Gợi ý laptop bán chạy dưới 20 triệu có voucher",
  "pageContext": {
    "productId": null,
    "categoryId": 2
  }
}
```

The response contains a Vietnamese answer plus structured product cards, voucher conditions, quick
replies, and correlation identifiers. The browser must render product/voucher fields from the
structured response instead of parsing the answer text.

## Trust boundaries

- Catalog results require an active product, active SKU, and positive aggregate available stock.
- Best sellers reuse the configured valid-order lookback and statuses from `RecommendationService`.
- Campaign offers require both an effective published campaign and an active, effective promotion.
- Exhausted global usage limits and authenticated per-user limits are excluded.
- Product/category coupon scopes are matched to each returned product. Cart coupons are labelled as
  cart-wide and are not represented as product-exclusive offers.
- Email addresses, Vietnamese phone numbers, and token-like secrets are removed before a prompt is
  sent to Gemini. Product/order/customer records are never included in the intent prompt.
- Redis conversations are owner-bound (`userId` or anonymous session) and expire after 24 hours by
  default.

## AI behavior and fallback

Gemini returns a small JSON intent contract (`BEST_SELLER`, `PRODUCT_DISCOVERY`,
`CAMPAIGN_OFFERS`) with query/category/budget/brand/attribute constraints. If Gemini is disabled,
not configured, unavailable, or returns invalid JSON, a deterministic Vietnamese parser handles the
same endpoint. AI failure therefore does not remove basic shopping discovery.

## Configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `FEATURE_CHAT_ASSISTANT_ENABLED` | `true` | Feature switch |
| `CHAT_AI_ENABLED` | `true` | Enable Gemini intent extraction |
| `GEMINI_API_KEY` | empty | Gemini credential; never commit a real value |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Configurable model identifier |
| `GEMINI_BASE_URL` | Google Generative Language v1beta | Provider endpoint |
| `CHAT_CONVERSATION_TTL` | `24h` | Redis conversation retention |
| `CHAT_MAX_PRODUCTS` | `6` | Maximum product cards per response |
| `CHAT_MAX_HISTORY_MESSAGES` | `12` | Bounded Redis conversation history |

## Analytics

The assistant emits `CHAT_MESSAGE` without raw message text. The storefront emits
`CHAT_PRODUCT_IMPRESSION`, `CHAT_PRODUCT_CLICK`, and `CHAT_VOUCHER_CLICK` using source
`CHAT_ASSISTANT`. Properties contain correlation IDs and campaign/code metadata only, never PII.

## Verification

- `RuleBasedChatIntentAnalyzerTest`: Vietnamese intent, budget, follow-up, clarification.
- `ChatAssistantServiceTest`: offer validity behavior, product cards, ownership, anonymous session.
- `ChatDataFoundationIntegrationTest`: PostgreSQL campaign/promotion/scope/time and in-stock query.
- `chat-assistant.service.spec.ts`: anonymous header and conversation persistence.
