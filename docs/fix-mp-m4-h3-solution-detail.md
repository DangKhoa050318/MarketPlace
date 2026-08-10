# Báo Cáo Giải Quyết Lỗi MP-M4 (Medium) & MP-H3 (High)

---

## MP-M4 — Client tự xác nhận thanh toán qua webhook

- **Mức độ**: Medium (kết hợp MP-C1 thành Critical end-to-end)
- **File tác động**: [`bank-transfer-dialog.component.ts`](../frontend/src/app/features/cart/bank-transfer-dialog/bank-transfer-dialog.component.ts)

### Nguyên nhân
Trong hàm `triggerMockBankTransfer()`, khi gọi PayGate API `/api/v1/bank-transfers/receive` thất bại, frontend có fallback **gọi thẳng** webhook endpoint `/api/v1/payments/paygate-webhook` từ browser — cho phép client tự đánh dấu đơn hàng `PAID` mà không cần PayGate xác nhận thật.

### Giải pháp
Xóa toàn bộ block fallback gọi webhook. Khi PayGate không phản hồi, chỉ hiển thị thông báo lỗi cho user.

```diff
       error: () => {
-        const webhookBody = { ... };
-        this.http.post('/api/v1/payments/paygate-webhook', webhookBody).subscribe({...});
+        this.simulating = false;
+        this.snackBar.open('Không thể kết nối PayGate. Vui lòng thử lại sau.', ...);
       }
```

---

## MP-H3 — HTTP ra ngoài không timeout, giữ transaction/lock

- **Mức độ**: High
- **File tác động**: [`PaygateClientServiceImpl.java`](../backend/src/main/java/com/training/marketplace/service/impl/PaygateClientServiceImpl.java)

### Nguyên nhân
`new RestTemplate()` mặc định không có connect/read timeout → nếu PayGate chậm hoặc không phản hồi, request treo vô hạn, giữ luôn DB connection và `@Transactional` lock.

### Giải pháp
Cấu hình `SimpleClientHttpRequestFactory` với timeout rõ ràng:

```diff
     public PaygateClientServiceImpl() {
-        this.restTemplate = new RestTemplate();
+        var factory = new SimpleClientHttpRequestFactory();
+        factory.setConnectTimeout(5_000);   // 5 seconds
+        factory.setReadTimeout(10_000);     // 10 seconds
+        this.restTemplate = new RestTemplate(factory);
     }
```

---

## Kết quả nghiệm thu

| Kiểm tra | Kết quả |
|---|---|
| Backend unit tests (`mvn clean test`) | `329/329 PASS` ✅ |
| Frontend build (`npm run build`) | **BUILD SUCCESS** ✅ |
| Grep webhook trong frontend | Không còn gọi `/api/v1/payments/paygate-webhook` từ client |
