# 🛠️ Phân công Fix Issue — Code Review MarketPlace

> **Nguồn:** Báo cáo review của mentor (`MarketPlace-Code-Review.md`, 2026-08-08).
> **Tổng:** 22 issue — 4 Critical · 5 High · 7 Medium · 6 Low.
> **Nguyên tắc chia:** theo **vùng chuyên môn/feature** của từng người + **cân bằng trọng số mức độ** (Critical=4, High=3, Medium=2, Low=1). Chia theo cụm file để ít đụng nhau khi merge.

---

## 1. Tổng quan phân công

| Người | Số issue | C / H / M / L | Trọng số | Chủ đề phụ trách |
|---|---|---|---|---|
| **HoangNQ** | 4 | 2 / 1 / 1 / 0 | 13 | Payment / Webhook (chuỗi giả mạo thanh toán) |
| **TriTVV** | 6 | 1 / 1 / 2 / 2 | 13 | Auth & Security config (backend) + CI |
| **GiangHV** | 6 | 0 / 3 / 1 / 2 | 13 | Frontend client-security + Recommendation |
| **KhoaNXD** | 6 | 1 / 0 / 4 / 1 | 12 | ai-service / Chatbot + Recommendation-cache + Coverage |

> **HoangNQ chỉ 4 issue nhưng gánh 2 Critical** vì **MP-C1 + MP-C3 + MP-M4** là **một chuỗi khai thác duy nhất** (đăng nhập → lấy apiKey lộ ở response → tự POST webhook → đơn PAID). Tách cho nhiều người sẽ vá vụn → cố ý gom về 1 người.

---

## 2. 🔵 HoangNQ — Payment / Webhook (ƯU TIÊN #1)

| ID | Mức | Vấn đề | Vị trí | Hướng fix gợi ý |
|---|---|---|---|---|
| **MP-C1** | 🔴 Critical | Verify webhook luôn pass (so `signature` với chính `merchantApiKey` server) | `PaymentWebhookServiceImpl.java:136-150` | Xác thực bằng **HMAC ký bởi PayGate** trên payload (đã có `SignatureValidationFilter`/`HmacUtils` bên PayGate develop), **không** so trực tiếp với apiKey cấu hình. Thêm check **order thuộc về user** gọi. |
| **MP-C3** | 🔴 Critical | Merchant apiKey hardcode lộ ra response client | `OrderServiceImpl.java:280,348,382,436` | **Bỏ apiKey khỏi response** trả về browser (chỉ backend↔backend cần). Đọc từ `@Value`/secret store, không literal. |
| **MP-M4** | 🟡 Medium | Client tự gọi webhook để tự xác nhận thanh toán | `bank-transfer-dialog.component.ts:~628-650` | FE **không** được tự đánh dấu PAID; chỉ backend/PayGate thật gọi webhook. Gỡ lời gọi webhook ở FE. |
| **MP-H3** | 🟠 High | `new RestTemplate()` không timeout → giữ tx/lock | `PaygateClientServiceImpl.java:44` | Cấu hình connect/read timeout (5-10s) qua `RestTemplateBuilder`; tách call ra ngoài `@Transactional` nếu có thể. |

**DoD:** thêm unit test `verifySignature_withApiKeyAsSignature_shouldNotPass()` (kỳ vọng `false`), test webhook cross-user trả 403, và test timeout PayGate.

---

## 3. 🟢 TriTVV — Auth & Security config (backend) + CI

| ID | Mức | Vấn đề | Vị trí | Hướng fix gợi ý |
|---|---|---|---|---|
| **MP-C2** | 🔴 Critical | JWT secret có default hardcode | `application.yml:60` | Bỏ default: `${JWT_SECRET}` (không fallback) + **fail-fast** khi thiếu (guard `@PostConstruct`/`@Value` bắt buộc). |
| **MP-H1** | 🟠 High | CORS `allowedOriginPatterns("*")` + `allowCredentials(true)` | `SecurityConfig.java:115-125` | Whitelist domain cụ thể (đọc từ `application.yml`), **bỏ `*`** khi bật credentials. Xoá cấu hình đè whitelist an toàn. |
| **MP-M1** | 🟡 Medium | Rate-limit đọc `SecurityContext` **trước** JWT filter → rỗng | `RateLimitingFilter.java:44-89` | Đặt rate-limit filter **sau** `jwtAuthenticationFilter`, hoặc key theo userId lấy từ token đã parse. |
| **MP-M5** | 🟡 Medium | Log DEBUG/SQL bật cứng, không theo profile | `application.yml:80-82` | Chuyển DEBUG/`show-sql` vào profile `dev`; profile mặc định/`prod` để INFO/WARN. |
| **MP-L1** | ⚪ Low | Actuator permitAll toàn `/actuator/**` | `SecurityConfig.java:41` | Chỉ expose `health,info`; các endpoint khác yêu cầu auth (ADMIN). |
| **MP-L2** | ⚪ Low | CI thiếu lint/frontend/integration test | `.github/workflows/backend-ci.yml` | Thêm step `ng lint`/`ng test`, checkstyle/spotbugs, và bật lại Testcontainers integration test. |

---

## 4. 🟣 GiangHV — Frontend client-security + Recommendation

| ID | Mức | Vấn đề | Vị trí | Hướng fix gợi ý |
|---|---|---|---|---|
| **MP-H2** | 🟠 High | JWT ở `localStorage` + chatbot `[innerHTML]` (XSS) | `auth.service.ts:68-95`, `chatbot-widget.component.ts:92-105` | Sanitize markdown LLM trước khi render (`DomSanitizer`); cân nhắc token qua httpOnly cookie hoặc in-memory. |
| **MP-H4** | 🟠 High | Admin guard chỉ check ở client | `admin.guard.ts:8,11` | UI guard chỉ là UX; đảm bảo **backend** có `@PreAuthorize`/role check độc lập trên mọi API admin. |
| **MP-H5** | 🟠 High | Config prod trỏ `localhost` | `environment.prod.ts:5-6` | Điền domain thật; kiểm tra `angular.json` `fileReplacements` hoạt động đúng cho `paygateApiUrl`. |
| **MP-M7** | 🟡 Medium | Race condition refresh-token interceptor | `jwt.interceptor.ts:33-64` | Reset `isRefreshing=false` + đẩy lỗi cho `refreshTokenSubject` ở **nhánh lỗi** để 5 request pending không treo. |
| **MP-L5** | ⚪ Low | Ảnh fallback hardcode domain ngoài | `recommendation-carousel.component.ts:352-353` | Dùng asset local làm fallback thay vì `images.unsplash.com`. |
| **MP-L6** | ⚪ Low | Session ID entropy thấp (`Math.random`) | `recommendation.service.ts:41-47` | Dùng `crypto.randomUUID()` + polyfill chuẩn thay `Math.random()`. |

---

## 5. 🟠 KhoaNXD — ai-service / Chatbot + Recommendation-cache + Coverage

| ID | Mức | Vấn đề | Vị trí | Hướng fix gợi ý |
|---|---|---|---|---|
| **MP-C4** | 🔴 Critical | ai-service `/api/chat` không auth/rate-limit + prompt injection | `ai-service/main.py:21-79` | Thêm shared-secret/JWT giữa backend↔ai-service; rate-limit; giới hạn độ dài message/history; sanitize `product_context`/`coupon_context` trước khi nối prompt. |
| **MP-M2** | 🟡 Medium | Similarity recommendation không cache | `SimilarProductRecommendationStrategy.java:73` | `@Cacheable` (Redis đã sẵn) cho `findAllByActiveTrue()`/kết quả tính; TTL hợp lý. |
| **MP-M3** | 🟡 Medium | Model/SDK Gemini nghi sai tên/lỗi thời | `ai-service/requirements.txt:4`, `main.py:19` | Kiểm chứng tên model + nâng SDK `google-generativeai` lên bản hỗ trợ; pin version. |
| **MP-M6** | 🟡 Medium | Lỗi ai-service lộ chi tiết (`detail=str(e)`) | `ai-service/main.py:77-79` | Trả message generic cho client, log chi tiết ở server. |
| **MP-L3** | ⚪ Low | `/health` lộ `api_key_configured` | `ai-service/main.py:83` | Chỉ trả `{"status":"ok"}` (không lộ trạng thái cấu hình). |
| **MP-L4** | ⚪ Low | Test coverage thấp (BE ~18.5% / FE ~14%) | toàn repo | Thêm unit test cho service quan trọng (`cart.service.ts`, `jwt.interceptor.ts`, recommendation...). Đặt ngưỡng coverage tối thiểu. |

---

## 6. Thứ tự ưu tiên (theo report §5)

1. **MP-C1 + MP-C3 + MP-M4** (HoangNQ) — chuỗi "giả mạo thanh toán end-to-end", **fix đầu tiên**.
2. **MP-C2** (TriTVV) — fail-fast khi thiếu `JWT_SECRET`.
3. **MP-C4** (KhoaNXD) — bảo vệ ai-service trước khi có traffic thật.
4. **MP-H1** (TriTVV) — siết CORS.
5. Nhóm High còn lại (**MP-H2 → H5**) theo mức ảnh hưởng client.
6. Medium → Low.

## 7. Quy trình

- Mỗi issue: **1 nhánh** `fix/mp-<id>-<mô-tả-ngắn>` off `dev` → PR về `dev`, gắn label mức độ + assignee.
- **DoD mỗi issue:** có **unit test tái hiện lỗi** (theo "Test case phát hiện lỗi" trong report) → xanh sau fix; `mvnw test '-Dtest=!*IntegrationTest'` pass; FE `ng build` pass nếu đụng frontend.
- Ràng buộc: các issue này **thuần MarketPlace**; nếu chạm điểm tích hợp với PayGate (VD verify HMAC ở MP-C1) thì **chỉ sửa phía MarketPlace**, chốt format chữ ký với team PayGate.

## 8. Không phải issue (report §4 — khỏi đụng)

Không SQL injection · `GlobalExceptionHandler` không leak stack trace · Flyway versioning nhất quán · recommendation eligibility 1 batch query (không N+1) · JWT refresh rotation+revocation qua Redis (đúng).

---

> Nguồn chi tiết + test case từng issue: xem `MarketPlace-Code-Review.md`. Bảng này chỉ tóm tắt phân công + hướng fix.
