# Báo Cáo Giải Quyết Lỗi Security MP-C3 (Critical)

- **Mã lỗi**: MP-C3 — Merchant API key hardcode lộ ra response client
- **Mức độ**: Critical
- **Nhánh git**: `fix/mp-c3-remove-hardcoded-api-key`
- **File tác động**:
  - [`PaygatePayloadResponse.java`](../backend/src/main/java/com/training/marketplace/dto/response/PaygatePayloadResponse.java)
  - [`PaymentServiceImpl.java`](../backend/src/main/java/com/training/marketplace/service/impl/PaymentServiceImpl.java)
  - [`OrderServiceImpl.java`](../backend/src/main/java/com/training/marketplace/service/impl/OrderServiceImpl.java)
  - [`order.model.ts`](../frontend/src/app/core/models/order.model.ts)

---

## 1. Nguyên nhân lỗi (Root Cause)

Chuỗi `"mock-merchant-api-key-123456"` được hardcode trực tiếp trong 4 vị trí tại `PaymentServiceImpl` (3 chỗ) và `OrderServiceImpl` (1 chỗ), truyền vào field `merchantId` của DTO `PaygatePayloadResponse`. DTO này được trả về cho browser qua API checkout, khiến API key hiển thị nguyên văn trong response JSON trên DevTools Network tab.

Kết hợp với **MP-C1** (webhook bypass), kẻ tấn công có thể lấy API key từ response → giả mạo webhook signature → chiếm hàng miễn phí.

---

## 2. Giải pháp khắc phục

1. **Xóa field `merchantId`** khỏi DTO `PaygatePayloadResponse` — API key không bao giờ được gửi cho client.
2. **Xóa 4 literal hardcode** `"mock-merchant-api-key-123456"` tại `PaymentServiceImpl` (3 chỗ) và `OrderServiceImpl` (1 chỗ).
3. **Xóa field `merchantId`** khỏi frontend interface `PaygatePayload` trong `order.model.ts`.
4. **API key chỉ sử dụng server-to-server** — đã inject qua `@Value("${marketplace.paygate.api-key}")` trong `PaygateClientServiceImpl` và `PaymentWebhookServiceImpl`.

---

## 3. Code Diff chính

### [`PaygatePayloadResponse.java`](../backend/src/main/java/com/training/marketplace/dto/response/PaygatePayloadResponse.java)

```diff
 public record PaygatePayloadResponse(
         Long orderId,
         Long customerId,
-        String merchantId,
         BigDecimal totalAmount,
         ...
```

### [`PaymentServiceImpl.java`](../backend/src/main/java/com/training/marketplace/service/impl/PaymentServiceImpl.java) (3 chỗ)

```diff
         return new PaygatePayloadResponse(
                 order.getId(),
                 order.getUser().getId(),
-                "mock-merchant-api-key-123456",
                 order.getTotalAmount(),
```

### [`order.model.ts`](../frontend/src/app/core/models/order.model.ts)

```diff
 export interface PaygatePayload {
   orderId: number;
   customerId: number;
-  merchantId: string;
   totalAmount: number;
```

---

## 4. Kết quả nghiệm thu (Verification)

### 4.1 Unit Test
```powershell
cd backend
.\mvnw.cmd test "-Dtest=!*IntegrationTest"
```
- **Kết quả**: `Tests run: 329, Failures: 0, Errors: 0, Skipped: 0` (**BUILD SUCCESS**)

### 4.2 Frontend Build
```powershell
cd frontend
npm run build
```
- **Kết quả**: **BUILD SUCCESS** (không có lỗi compile)

### 4.3 Xác nhận API key không còn lộ ra client
```powershell
grep -rn "mock-merchant-api-key-123456" backend/src/main/java/
```
- **Kết quả**: Chỉ còn 1 kết quả duy nhất trong `PaymentWebhookServiceImpl.java` (dùng làm default value cho `@Value`, chỉ phía server).
