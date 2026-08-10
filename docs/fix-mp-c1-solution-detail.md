# Báo Cáo Giải Quyết Lỗi Security MP-C1 (Critical)

- **Mã lỗi**: MP-C1 — Verify webhook thanh toán luôn pass vô điều kiện
- **Mức độ**: Critical
- **Nhánh git**: `fix/mp-c1-payment-webhook-verification`
- **File tác động**:
  - [`PaymentWebhookController.java`](../backend/src/main/java/com/training/marketplace/controller/PaymentWebhookController.java)
  - [`PaymentWebhookServiceImpl.java`](../backend/src/main/java/com/training/marketplace/service/impl/PaymentWebhookServiceImpl.java)
  - [`PaymentWebhookServiceTest.java`](../backend/src/test/java/com/training/marketplace/service/PaymentWebhookServiceTest.java)

---

## 1. Nguyên nhân lỗi (Root Cause)

Code cũ kiểm tra `X-Signature` bằng cách chấp nhận so sánh trực tiếp với `merchantApiKey` thô (`constantTimeEquals(signature, merchantApiKey)`). 
Kẻ gian có thể lấy `merchantApiKey` (bị rò rỉ ở client) và tự gọi `POST /api/v1/payments/paygate-webhook` với `X-Signature: <merchantApiKey>` để chuyển trạng thái đơn hàng bất kỳ thành `PAID` mà không cần thanh toán thật.

---

## 2. Giải pháp khắc phục

1. **Strict HMAC Verification**: `X-Signature` bắt buộc phải trùng khớp với chuỗi chữ ký HMAC-SHA256 Base64 được tính toán từ `rawPayload` (Body JSON) và `merchantApiKey`.
2. **Bỏ Bypass**: Xóa bỏ hoàn toàn điều kiện chấp nhận `signature == merchantApiKey` thô.
3. **Bắt buộc Request Body**: Trả về `403 Forbidden` ngay lập tức nếu `rawPayload` rỗng hoặc null.

---

## 3. Code Diff chính

### [`PaymentWebhookServiceImpl.java`](../backend/src/main/java/com/training/marketplace/service/impl/PaymentWebhookServiceImpl.java)

```diff
     try {
         if (rawPayload == null || rawPayload.isBlank()) {
-            if (constantTimeEquals(signature, merchantApiKey)) {
-                return;
-            }
+            log.warn("Rejected PayGate webhook: missing raw payload for HMAC signature calculation");
             throw new ForbiddenException("Invalid webhook signature / unauthorized request");
         }

         String expectedSignature = com.training.marketplace.utils.HmacUtils.generateSignature(rawPayload, merchantApiKey);
-        if (!constantTimeEquals(signature, expectedSignature) && !constantTimeEquals(signature, merchantApiKey)) {
+        if (!constantTimeEquals(signature, expectedSignature)) {
             log.warn("Rejected PayGate webhook: invalid X-Signature. Expected {}, got {}", expectedSignature, signature);
             throw new ForbiddenException("Invalid webhook signature / unauthorized request");
         }
```

---

## 4. Kết quả nghiệm thu (Verification)

### 4.1 Unit Test
```powershell
cd backend
.\mvnw.cmd test -Dtest=PaymentWebhook*Test
```
- **Kết quả**: `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0` (**BUILD SUCCESS**)

### 4.2 API Test thực tế trên Server (`http://localhost:8080`)
1. **Truyền Raw API Key** (`X-Signature: mock-merchant-api-key-123456`) ➔ **`HTTP 403 Forbidden`** *(Đã chặn đứng kịch bản tấn công)*.
2. **Truyền HMAC Signature chuẩn** (`X-Signature: <Base64_HMAC>`) ➔ **`HTTP 200 OK`** *(Đơn hàng cập nhật CONFIRMED & PAID thành công)*.
