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
replies, optional order/payment details, and correlation identifiers. The browser must render product/voucher fields from the
structured response instead of parsing the answer text.

## Cart, coupon and checkout commands

- The storefront recognizes Vietnamese add-to-cart commands against the latest recommendation and
  adds the recommended `variantId` through the existing cart API.
- Voucher commands preview the code against the authoritative backend cart. The code is only
  consumed and its usage recorded when `OrderService` creates the order.
- Stateful checkout supports both `COD` and `CREDIT_CARD` (`PayGate E-Wallet / Card Gateway`). The
  assistant collects payment method, shipping address and optional delivery note before calling the
  existing order service.
- A credit-card order returns the PayGate `paymentUrl` in the structured order summary. The
  storefront opens it in a new tab so the original chat remains active.
- After the user starts PayGate payment, the storefront polls the authenticated order endpoint every
  two seconds. It writes success into chat only when the server reports `paymentStatus=PAID`, and
  writes failure/cancellation only when the server reports `status=CANCELLED`. These terminal states
  are set by the signed PayGate webhook; redirect query parameters are not trusted as payment truth.
- The pending order ID is retained in browser storage so status tracking resumes after a reload or
  after PayGate redirects its tab back to the Marketplace callback page.
- Bank transfer and PayGate BNPL remain available through the regular checkout page, not directly
  through the chat state machine.

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
- `ChatAssistantServiceTest`: COD and PayGate card state machines, including returned payment URL.
- `chat-payment-status.service.spec.ts`: paid/cancelled server truth and pending-payment resume.
- `chat-assistant.component.spec.ts`: add-to-cart, voucher apply and chat payment notification.
