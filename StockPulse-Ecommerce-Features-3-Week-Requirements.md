# StockPulse / Marketplace — E-commerce Features & Weekly Requirements

> **Phiên bản:** 2.1  
> **Ngày:** 27/07/2026  

---

## Feature Scope

### FEATURE-STP-01 — Reviews, Ratings & Product Questions

Khách hàng đã mua sản phẩm có thể chấm điểm và viết nhận xét. Người dùng có thể đặt câu hỏi về sản phẩm; quản trị viên hoặc nhân viên được phân quyền có thể trả lời và kiểm duyệt nội dung.

**Kết quả mong đợi:**

- Chỉ người dùng đủ điều kiện mới được tạo đánh giá gắn nhãn `Verified Purchase`.
- Điểm trung bình, tổng số đánh giá và phân bố số sao được cập nhật chính xác.
- Khách hàng xem, lọc và sắp xếp đánh giá trên trang sản phẩm.
- Người dùng có thể đặt câu hỏi; câu trả lời chính thức được phân biệt rõ.
- Nội dung vi phạm có thể bị ẩn nhưng vẫn được lưu để audit.
- Mọi thao tác kiểm duyệt đều ghi nhận người thực hiện, thời điểm và lý do.

### FEATURE-STP-02 — Promotions & Merchandising

Quản trị viên tạo chương trình khuyến mãi, mã giảm giá, collection và banner để thúc đẩy khám phá sản phẩm và chuyển đổi mua hàng.

**Kết quả mong đợi:**

- Coupon hỗ trợ giảm theo phần trăm hoặc số tiền với điều kiện rõ ràng.
- Có thể giới hạn coupon theo thời gian, người dùng, sản phẩm, danh mục và tổng lượt sử dụng.
- Backend là nguồn tính giảm giá cuối cùng và chống sử dụng vượt giới hạn.
- Quản trị viên quản lý campaign, collection, banner và thứ tự sản phẩm.
- Nội dung merchandising tự động bật/tắt theo lịch đã cấu hình.
- Có dữ liệu đo lường lượt hiển thị, click và chuyển đổi của campaign.

### FEATURE-STP-03 — Product Recommendations

Hệ thống cung cấp các khối gợi ý sản phẩm theo ngữ cảnh và hành vi, bắt đầu bằng rule-based rồi bổ sung cá nhân hóa khi có đủ dữ liệu.

**Kết quả mong đợi:**

- Hỗ trợ sản phẩm tương tự, bán chạy, thường xem cùng và thường mua cùng.
- Cá nhân hóa dựa trên lịch sử xem, click, wishlist và mua hàng khi người dùng đồng ý.
- Không gợi ý sản phẩm bị ẩn, ngừng bán hoặc không đủ điều kiện hiển thị.
- Có fallback ổn định khi dữ liệu cá nhân không đủ hoặc dịch vụ gợi ý lỗi.
- Ghi nhận impression, click và add-to-cart để đo hiệu quả.
- Hệ thống gợi ý không được tự thay đổi giá, coupon, giỏ hàng hoặc đơn hàng.

### FEATURE-STP-04 — Customer Experience & Analytics

Hệ thống thu thập sự kiện thương mại điện tử có kiểm soát và cung cấp dashboard giúp quản trị viên hiểu hành trình khách hàng, hiệu quả nội dung và điểm rơi chuyển đổi.

**Kết quả mong đợi:**

- Theo dõi các bước xem sản phẩm, thêm vào giỏ, bắt đầu checkout và tạo đơn.
- Cung cấp wishlist và sản phẩm đã xem gần đây cho khách hàng.
- Hiển thị conversion funnel, sản phẩm bán chạy và nội dung có tương tác thấp.
- Phân tích hiệu quả promotion, recommendation, review và câu hỏi sản phẩm.
- Hỗ trợ lọc theo thời gian, danh mục, sản phẩm và campaign.
- Cho phép xuất dữ liệu tổng hợp; không xuất dữ liệu cá nhân nhạy cảm.

### FEATURE-STP-05 — Smart Bundle Builder

Hệ thống cho phép quản trị viên định nghĩa các mẫu bundle và giúp khách hàng tự xây dựng một bộ sản phẩm phù hợp với nhu cầu, ngân sách và các điều kiện tương thích.

**Kết quả mong đợi:**

- Hỗ trợ fixed bundle và smart bundle theo các slot/quy tắc sản phẩm.
- Khách hàng trả lời câu hỏi ngắn về nhu cầu, sở thích và ngân sách để nhận bundle gợi ý.
- Cho phép thay thế từng sản phẩm nhưng vẫn bảo đảm các ràng buộc của bundle.
- Backend tính giá, mức tiết kiệm và kiểm tra eligibility của từng thành phần.
- Khách có thể lưu, chia sẻ hoặc thêm toàn bộ bundle vào giỏ hàng.
- Ghi nhận impression, replace item, save, share và add-to-cart để đo hiệu quả.
- Feature chỉ tổ chức sản phẩm và trải nghiệm mua sắm; không bao gồm nhập hàng hoặc logistics.

### FEATURE-STP-06 — Product Comparison Studio

Khách hàng chọn nhiều sản phẩm và xem bảng so sánh chuẩn hóa, các điểm khác biệt quan trọng, review insight và gợi ý sản phẩm phù hợp với ưu tiên đã chọn.

**Kết quả mong đợi:**

- Cho phép so sánh tối đa số sản phẩm được cấu hình trong cùng một nhóm tương thích.
- Chuẩn hóa thuộc tính và đơn vị để các giá trị có thể so sánh trực tiếp.
- Làm nổi bật điểm giống nhau, khác nhau và dữ liệu còn thiếu.
- Kết hợp giá, promotion, rating, review insight và thuộc tính sản phẩm.
- Cho phép khách chọn ưu tiên để nhận giải thích “phù hợp nhất” mà không che giấu trade-off.
- Hỗ trợ lưu/chia sẻ comparison bằng token không chứa dữ liệu cá nhân.
- Ghi nhận hành vi thêm/xóa sản phẩm, thay đổi ưu tiên và click từ comparison.

---

## Requirement Conventions

- Mã `REQ-STP-B-*`: Backend, dữ liệu, API, scheduled job và xử lý nghiệp vụ.
- Mã `REQ-STP-F-*`: Angular và trải nghiệm người dùng.
- Mã `REQ-STP-T-*`: unit, integration, component, E2E, performance và security testing.
- Mã `REQ-STP-X-*`: acceptance criteria dùng chung cho nhiều feature.
- Mỗi requirement có đúng một owner chính; owner theo dõi requirement đến khi đạt Definition of Done.
- Mọi API danh sách phải có phân trang, giới hạn kích thước trang và sắp xếp ổn định.
- Mọi số tiền và mức giảm phải được tính bằng kiểu thập phân chính xác ở backend.
- Thời gian được lưu theo UTC và hiển thị theo múi giờ đã cấu hình.
- Mọi thao tác quản trị hoặc kiểm duyệt quan trọng phải có audit log.

---

## 3-Week Delivery Requirements

> **Cam kết phạm vi:** Toàn bộ requirement của baseline được giữ nguyên. Kế hoạch triển khai có đúng **3 tuần**, mỗi tuần chạy 4 workstream song song, tương ứng với năng lực của 4 thành viên.
>
> **Nguyên tắc phân công:** Bốn feature đầu có một owner xuyên suốt: GiangHV9 — FEATURE-STP-01, KhoaNXD1 — FEATURE-STP-02, HoangNQ17 — FEATURE-STP-03, TriTVV2 — FEATURE-STP-04. Hai feature sau được ghép cặp: GiangHV9/KhoaNXD1 cùng làm FEATURE-STP-05 và HoangNQ17/TriTVV2 cùng làm FEATURE-STP-06.
>
> **Trách nhiệm nghiệm thu:** Dù làm theo cặp, mỗi requirement vẫn có đúng một owner chính theo bảng Assignment trong từng workstream; partner chịu trách nhiệm review, hỗ trợ tích hợp và thay thế khi cần. Các dependency phải được chốt bằng API contract, event schema, migration order và feature flag ngay đầu tuần.
>
> **Điều kiện lịch:** Business Decisions liên quan phải được Product Owner/Team Lead chốt trước workstream phụ thuộc. Requirement chưa đạt Definition of Done không được xem là hoàn thành chỉ vì kết thúc tuần.

### Week 1 — Foundation cho 4 feature độc lập

**Mục tiêu tuần:** Mỗi thành viên triển khai phần nền tảng của feature mình sở hữu: Reviews/Ratings, Promotion Rules, Rule-based Recommendations và Customer Experience Tracking.

**Exit gate:** Data model, API contract, migration, UI cốt lõi và bộ test nền tảng của FEATURE-STP-01 đến FEATURE-STP-04 hoàn thành; contract cho tuần 2 được chốt.

#### Workstream 1 — FEATURE-STP-01 — Reviews & Ratings Foundation

> Nguồn đối chiếu: Gói yêu cầu baseline số 1. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-01 — Reviews & Ratings Foundation.

**Mục tiêu tuần:** Hoàn thiện dữ liệu, API và giao diện cốt lõi cho đánh giá, xếp hạng và điều kiện `Verified Purchase`.

#### Backend

- [x] REQ-STP-B-101: Tạo `ProductReview` gồm user, product, order item tham chiếu, rating 1–5, tiêu đề, nội dung, trạng thái và timestamps.
- [x] REQ-STP-B-102: Tạo migration với ràng buộc rating 1–5 và khóa duy nhất theo chính sách một người dùng/một sản phẩm.
- [x] REQ-STP-B-103: Xác định eligibility từ dữ liệu đơn hàng ở backend; client không được tự khai báo `Verified Purchase`.
- [x] REQ-STP-B-104: Cung cấp API tạo, cập nhật và xóa mềm đánh giá của chính người dùng.
- [x] REQ-STP-B-105: Cung cấp API danh sách đánh giá công khai theo sản phẩm với phân trang, lọc số sao và sắp xếp.
- [x] REQ-STP-B-106: Tính và trả rating summary gồm điểm trung bình, tổng lượt đánh giá và số lượng theo từng mức sao.
- [x] REQ-STP-B-107: Cập nhật rating summary chính xác khi đánh giá được tạo, sửa, xóa mềm, ẩn hoặc hiện lại.
- [x] REQ-STP-B-108: Chuẩn hóa và giới hạn độ dài tiêu đề/nội dung; từ chối payload rỗng hoặc vượt giới hạn.

#### Angular

- [x] REQ-STP-F-101: Tạo `RatingSummaryComponent` hiển thị điểm trung bình, tổng lượt đánh giá và phân bố 1–5 sao.
- [x] REQ-STP-F-102: Tạo `ReviewListComponent` có phân trang, lọc theo số sao và sắp xếp mới nhất/hữu ích.
- [x] REQ-STP-F-103: Tạo form đánh giá với star selector, tiêu đề, nội dung, bộ đếm ký tự và validation.
- [x] REQ-STP-F-104: Chỉ hiển thị hành động tạo/sửa đánh giá dựa trên eligibility do backend trả về.
- [x] REQ-STP-F-105: Hiển thị nhãn `Verified Purchase`, thời điểm và trạng thái đã chỉnh sửa trên đánh giá.
- [x] REQ-STP-F-106: Hiển thị loading, empty state, lỗi tải dữ liệu và retry mà không làm lỗi toàn bộ trang sản phẩm.

#### Testing

- [x] REQ-STP-T-101: Viết unit test cho rating biên 1/5, rating ngoài phạm vi và validation nội dung.
- [x] REQ-STP-T-102: Viết integration test cho người dùng đủ/không đủ điều kiện đánh giá và client giả mạo verified purchase.
- [x] REQ-STP-T-103: Viết test cho ràng buộc đánh giá trùng và hành vi cập nhật theo chính sách.
- [x] REQ-STP-T-104: Viết test rating summary khi tạo, sửa, xóa mềm, ẩn và hiện đánh giá.
- [x] REQ-STP-T-105: Viết Angular component test cho summary, filter, form validation và các trạng thái UI.
- [x] REQ-STP-T-106: Viết E2E test khách đã mua tạo đánh giá, sửa đánh giá và thấy dữ liệu cập nhật trên trang sản phẩm.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **HoangNQ17** | Các thành viên còn lại review khi có dependency | Toàn bộ requirement REQ-STP-B-101 → REQ-STP-B-108, REQ-STP-F-101 → REQ-STP-F-106 và REQ-STP-T-101 → REQ-STP-T-106 | Reviews, Ratings foundation |

#### Workstream 2 — FEATURE-STP-02 — Promotion Rules & Coupon Engine

> Nguồn đối chiếu: Gói yêu cầu baseline số 3. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-02 — Promotion Rules & Coupon Engine.

**Mục tiêu tuần:** Hoàn thiện mô hình coupon, kiểm tra điều kiện và tính mức giảm an toàn tại backend.

#### Backend

- [ ] REQ-STP-B-301: Tạo `PromotionCode` gồm mã duy nhất, loại giảm, giá trị, mức giảm tối đa, đơn tối thiểu, thời gian hiệu lực và trạng thái.
- [ ] REQ-STP-B-302: Hỗ trợ giới hạn tổng lượt dùng và giới hạn lượt dùng trên mỗi khách hàng.
- [ ] REQ-STP-B-303: Hỗ trợ phạm vi áp dụng theo toàn giỏ, sản phẩm hoặc danh mục; lưu quan hệ phạm vi bằng cấu trúc có thể mở rộng.
- [ ] REQ-STP-B-304: Chuẩn hóa mã coupon không phân biệt chữ hoa/thường và từ chối mã trùng.
- [ ] REQ-STP-B-305: Tạo `PromotionValidationService` kiểm tra trạng thái, thời gian, khách hàng, phạm vi, đơn tối thiểu và giới hạn sử dụng.
- [ ] REQ-STP-B-306: Tạo `DiscountCalculationService` tính giảm theo phần trăm/số tiền, áp dụng trần giảm và không làm tổng đơn âm.
- [ ] REQ-STP-B-307: Cung cấp API preview coupon; backend tự đọc giỏ hàng hiện tại thay vì tin subtotal hoặc discount do client gửi.
- [ ] REQ-STP-B-308: Lưu kết quả sử dụng coupon theo nguyên tắc atomic để không vượt giới hạn khi nhiều yêu cầu đồng thời.

#### Angular

- [ ] REQ-STP-F-301: Tạo danh sách coupon có tìm kiếm, lọc theo trạng thái/loại và phân trang.
- [ ] REQ-STP-F-302: Tạo form coupon với validation loại giảm, giá trị, trần giảm, đơn tối thiểu, thời gian và giới hạn sử dụng.
- [ ] REQ-STP-F-303: Tạo bộ chọn phạm vi sản phẩm/danh mục có tìm kiếm và hiển thị số mục đã chọn.
- [ ] REQ-STP-F-304: Bổ sung ô nhập coupon tại giỏ hàng/checkout và gọi API preview.
- [ ] REQ-STP-F-305: Hiển thị mã đã áp dụng, số tiền giảm, tổng mới và lý do cụ thể khi coupon không hợp lệ.
- [ ] REQ-STP-F-306: Khi nội dung giỏ hàng thay đổi, yêu cầu backend kiểm tra lại coupon và cập nhật tổng tiền.

#### Testing

- [ ] REQ-STP-T-301: Viết unit test cho coupon theo phần trăm, số tiền, trần giảm, đơn tối thiểu và tổng không âm.
- [ ] REQ-STP-T-302: Viết test cho coupon chưa bắt đầu, hết hạn, bị vô hiệu hóa và mã không phân biệt hoa/thường.
- [ ] REQ-STP-T-303: Viết test phạm vi toàn giỏ, sản phẩm, danh mục và giỏ có cả sản phẩm hợp lệ/không hợp lệ.
- [ ] REQ-STP-T-304: Viết concurrency test tại lượt dùng cuối và xác nhận không vượt tổng giới hạn.
- [ ] REQ-STP-T-305: Viết security test xác nhận client không thể sửa subtotal, discount hoặc phạm vi coupon.
- [ ] REQ-STP-T-306: Viết E2E test quản trị viên tạo coupon → khách áp dụng → thay đổi giỏ → checkout giữ đúng snapshot giảm giá.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **KhoaNXD1** | Các thành viên còn lại review khi có dependency | Toàn bộ requirement REQ-STP-B-301 → REQ-STP-B-308, REQ-STP-F-301 → REQ-STP-F-306 và REQ-STP-T-301 → REQ-STP-T-306 | Promotion Rules và Coupon Engine |

#### Workstream 3 — FEATURE-STP-03 — Recommendation Events & Rule-based Recommendations

> Nguồn đối chiếu: Gói yêu cầu baseline số 5. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-03 — Recommendation Events & Rule-based Recommendations.

**Mục tiêu tuần:** Xây dựng dữ liệu hành vi và các chiến lược gợi ý rule-based làm baseline ổn định.

#### Backend

- [x] REQ-STP-B-501: Định nghĩa schema sự kiện `PRODUCT_VIEW`, `RECOMMENDATION_IMPRESSION`, `RECOMMENDATION_CLICK`, `ADD_TO_CART`, `PURCHASE`.
- [x] REQ-STP-B-502: Tạo endpoint thu nhận sự kiện có event ID, session ID, user ID tùy chọn, product ID, source, placement và timestamp.
- [x] REQ-STP-B-503: Validation event type, product, placement và timestamp; chống ghi trùng theo event ID.
- [x] REQ-STP-B-504: Tạo interface `RecommendationStrategy` để tách thuật toán gợi ý khỏi API/controller.
- [x] REQ-STP-B-505: Xây dựng chiến lược sản phẩm tương tự dựa trên danh mục, thương hiệu, thuộc tính và khoảng giá.
- [x] REQ-STP-B-506: Xây dựng chiến lược sản phẩm bán chạy theo số đơn hợp lệ trong khoảng thời gian cấu hình được.
- [x] REQ-STP-B-507: Xây dựng chiến lược thường xem cùng/mua cùng từ dữ liệu đồng xuất hiện tối thiểu.
- [x] REQ-STP-B-508: Lọc sản phẩm bị ẩn, ngừng bán, chính sản phẩm nguồn và sản phẩm trùng trước khi trả kết quả.

#### Angular

- [x] REQ-STP-F-501: Tạo `RecommendationCarouselComponent` dùng lại được với tiêu đề, placement và danh sách sản phẩm.
- [x] REQ-STP-F-502: Tích hợp khối sản phẩm tương tự và thường xem cùng trên trang chi tiết sản phẩm.
- [x] REQ-STP-F-503: Tích hợp khối bán chạy trên trang chủ và trang danh mục.
- [x] REQ-STP-F-504: Gửi impression khi item đạt điều kiện hiển thị và click kèm placement/source.
- [x] REQ-STP-F-505: Hiển thị skeleton, empty state và ẩn toàn bộ khối khi API không có kết quả.
- [x] REQ-STP-F-506: Bảo đảm carousel hỗ trợ bàn phím, screen reader và responsive.

#### Testing

- [x] REQ-STP-T-501: Viết contract test cho schema sự kiện hợp lệ, thiếu trường, event type sai và event trùng.
- [x] REQ-STP-T-502: Viết unit test cho chiến lược similar products với danh mục, thuộc tính và khoảng giá.
- [x] REQ-STP-T-503: Viết unit test cho best sellers, co-view và co-purchase với dữ liệu rỗng/ít/nhiều.
- [x] REQ-STP-T-504: Viết test xác nhận kết quả loại sản phẩm ẩn, ngừng bán, nguồn và sản phẩm trùng.
- [x] REQ-STP-T-505: Viết Angular component test cho carousel, tracking, empty state và accessibility.
- [x] REQ-STP-T-506: Viết E2E test mở sản phẩm → thấy recommendation → click → add-to-cart và ghi nhận đúng chuỗi sự kiện.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **GiangHV9** | Các thành viên còn lại review khi có dependency | Toàn bộ requirement REQ-STP-B-501 → REQ-STP-B-508, REQ-STP-F-501 → REQ-STP-F-506 và REQ-STP-T-501 → REQ-STP-T-506 | Recommendation foundation và tracking |

#### Workstream 4 — FEATURE-STP-04 — Customer Experience Tracking, Wishlist & Funnel

> Nguồn đối chiếu: Gói yêu cầu baseline số 7. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-04 — Customer Experience Tracking, Wishlist & Funnel.

**Mục tiêu tuần:** Hoàn thiện tracking journey, wishlist, recently viewed và dữ liệu funnel có kiểm soát quyền riêng tư.

#### Backend

- [ ] REQ-STP-B-701: Chuẩn hóa taxonomy cho các sự kiện `PAGE_VIEW`, `PRODUCT_VIEW`, `SEARCH`, `ADD_TO_WISHLIST`, `ADD_TO_CART`, `BEGIN_CHECKOUT`, `ORDER_CREATED`.
- [ ] REQ-STP-B-702: Tạo ingestion pipeline validate schema, giới hạn kích thước batch, chống event trùng và ghi nhận ingestion status.
- [ ] REQ-STP-B-703: Tạo `Wishlist` theo user với ràng buộc duy nhất user/product và API thêm, xóa, kiểm tra trạng thái.
- [ ] REQ-STP-B-704: Tạo recently viewed theo user/session, giới hạn số mục, loại trùng và sắp xếp mới nhất.
- [ ] REQ-STP-B-705: Hỗ trợ merge wishlist/recently viewed từ session ẩn danh sang tài khoản sau đăng nhập theo chính sách.
- [ ] REQ-STP-B-706: Tổng hợp funnel theo session/user ẩn danh hóa và các bước product view → add-to-cart → begin checkout → order created.
- [ ] REQ-STP-B-707: Tạo retention policy xóa/anonymize raw events sau thời hạn cấu hình nhưng giữ aggregate không định danh.
- [ ] REQ-STP-B-708: Cung cấp API funnel summary theo khoảng ngày, danh mục, sản phẩm, campaign và device type.

#### Angular

- [ ] REQ-STP-F-701: Bổ sung nút wishlist trên product card và trang chi tiết với trạng thái đồng bộ từ backend.
- [ ] REQ-STP-F-702: Tạo trang wishlist có phân trang, xóa sản phẩm và chuyển sản phẩm vào giỏ.
- [ ] REQ-STP-F-703: Tạo khối recently viewed và cho phép người dùng xóa lịch sử xem.
- [ ] REQ-STP-F-704: Tích hợp tracking journey tại các điểm page view, search, product view, wishlist, cart và checkout.
- [ ] REQ-STP-F-705: Không gửi sự kiện không thiết yếu trước consent và dừng gửi sau khi người dùng opt-out.
- [ ] REQ-STP-F-706: Tạo funnel visualization cơ bản với số lượng, tỷ lệ chuyển đổi và drop-off giữa các bước.

#### Testing

- [ ] REQ-STP-T-701: Viết contract test cho từng event type, batch limit, timestamp sai và event trùng.
- [ ] REQ-STP-T-702: Viết integration test wishlist thêm/xóa/trùng và quyền truy cập theo user.
- [ ] REQ-STP-T-703: Viết test recently viewed loại trùng, giới hạn số mục, thứ tự và merge sau đăng nhập.
- [ ] REQ-STP-T-704: Viết test funnel aggregation với session hoàn tất, bỏ dở, event sai thứ tự và event trùng.
- [ ] REQ-STP-T-705: Viết privacy test cho consent, opt-out, retention và anonymization.
- [ ] REQ-STP-T-706: Viết E2E test xem sản phẩm → wishlist → cart → begin checkout → order created → dashboard ghi nhận funnel.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **TriTVV2** | Các thành viên còn lại review khi có dependency | Toàn bộ requirement REQ-STP-B-701 → REQ-STP-B-708, REQ-STP-F-701 → REQ-STP-F-706 và REQ-STP-T-701 → REQ-STP-T-706 | Event pipeline, wishlist và recently viewed |

### Week 2 — Hoàn thiện 4 feature độc lập

**Mục tiêu tuần:** Mỗi thành viên hoàn thiện feature mình sở hữu: Q&A/Moderation, Campaigns/Merchandising, Personalized Recommendations và Analytics Dashboard/Export.

**Exit gate:** FEATURE-STP-01 đến FEATURE-STP-04 đạt Definition of Done, chạy E2E, có audit/analytics phù hợp và sẵn sàng tích hợp với Bundle/Comparison.

#### Workstream 1 — FEATURE-STP-01 — Product Questions & Content Moderation

> Nguồn đối chiếu: Gói yêu cầu baseline số 2. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-01 — Product Questions & Content Moderation.

**Mục tiêu tuần:** Hoàn thiện hỏi đáp sản phẩm, bình chọn hữu ích và quy trình kiểm duyệt review/question/answer.

#### Backend

- [ ] REQ-STP-B-201: Tạo `ProductQuestion` gồm user, product, nội dung, trạng thái và timestamps.
- [ ] REQ-STP-B-202: Tạo `ProductAnswer` liên kết question, tác giả, nội dung và cờ câu trả lời chính thức.
- [ ] REQ-STP-B-203: Cung cấp API tạo câu hỏi, tạo câu trả lời và lấy thread hỏi đáp theo sản phẩm có phân trang.
- [ ] REQ-STP-B-204: Chỉ vai trò được cấu hình mới có thể đánh dấu câu trả lời là chính thức.
- [ ] REQ-STP-B-205: Tạo cơ chế người dùng đánh dấu review/question/answer là hữu ích; mỗi người chỉ có một vote hiện hành trên mỗi nội dung.
- [ ] REQ-STP-B-206: Tạo workflow kiểm duyệt `VISIBLE`, `HIDDEN`, `PENDING_REVIEW`, `REJECTED` cho review, question và answer.
- [ ] REQ-STP-B-207: Yêu cầu lý do khi ẩn hoặc từ chối nội dung và ghi audit log cho mọi thay đổi kiểm duyệt.
- [ ] REQ-STP-B-208: Không trả nội dung bị ẩn/từ chối trong API công khai nhưng vẫn cho quản trị viên có quyền tra cứu.

#### Angular

- [ ] REQ-STP-F-201: Tạo `ProductQuestionListComponent` hiển thị câu hỏi, câu trả lời và câu trả lời chính thức.
- [ ] REQ-STP-F-202: Tạo form đặt câu hỏi và form trả lời với validation và bộ đếm ký tự.
- [ ] REQ-STP-F-203: Bổ sung hành động “Hữu ích” trên review, question và answer; cập nhật số đếm sau phản hồi backend.
- [ ] REQ-STP-F-204: Tạo màn hình moderation queue có lọc theo loại nội dung, trạng thái, sản phẩm và khoảng ngày.
- [ ] REQ-STP-F-205: Tạo dialog ẩn/từ chối nội dung bắt buộc nhập lý do và hiển thị kết quả xử lý.
- [ ] REQ-STP-F-206: Hiển thị lịch sử kiểm duyệt và chỉ cung cấp hành động phù hợp với quyền người dùng.

#### Testing

- [ ] REQ-STP-T-201: Viết unit test cho validation question/answer và chuyển trạng thái moderation.
- [ ] REQ-STP-T-202: Viết authorization test cho tạo câu trả lời chính thức và truy cập nội dung bị ẩn.
- [ ] REQ-STP-T-203: Viết test vote hữu ích lặp, bỏ vote và nhiều người vote đồng thời.
- [ ] REQ-STP-T-204: Viết integration test xác nhận API công khai không trả nội dung bị ẩn hoặc từ chối.
- [ ] REQ-STP-T-205: Viết Angular component test cho thread hỏi đáp, form, vote và moderation dialog.
- [ ] REQ-STP-T-206: Viết E2E test đặt câu hỏi → trả lời chính thức → bình chọn hữu ích → quản trị viên kiểm duyệt.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **GiangHV9** | Các thành viên còn lại review khi có dependency | Toàn bộ requirement REQ-STP-B-201 → REQ-STP-B-208, REQ-STP-F-201 → REQ-STP-F-206 và REQ-STP-T-201 → REQ-STP-T-206 | Product Questions và Content Moderation |

#### Workstream 2 — FEATURE-STP-02 — Campaigns, Collections & Merchandising

> Nguồn đối chiếu: Gói yêu cầu baseline số 4. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-02 — Campaigns, Collections & Merchandising.

**Mục tiêu tuần:** Hoàn thiện campaign, collection, banner, lịch hiển thị và đo lường hiệu quả merchandising.

#### Backend

- [ ] REQ-STP-B-401: Tạo `Campaign` gồm tên, mô tả, thời gian bắt đầu/kết thúc, trạng thái và liên kết promotion.
- [ ] REQ-STP-B-402: Tạo `ProductCollection` hỗ trợ danh sách sản phẩm thủ công, thứ tự hiển thị và trạng thái xuất bản.
- [ ] REQ-STP-B-403: Tạo `MerchandisingBanner` gồm hình ảnh desktop/mobile, nội dung thay thế, target URL, vị trí, lịch và trạng thái.
- [ ] REQ-STP-B-404: Cung cấp CRUD và API công khai cho campaign, collection và banner đang hiệu lực.
- [ ] REQ-STP-B-405: Tự xác định hiệu lực từ trạng thái và khoảng thời gian ở backend; client không được ép hiển thị nội dung hết hạn.
- [ ] REQ-STP-B-406: Bảo đảm thứ tự sản phẩm trong collection là duy nhất và cập nhật reorder trong một transaction.
- [ ] REQ-STP-B-407: Ghi sự kiện impression/click cho banner, collection và campaign bằng event ID chống ghi trùng.
- [ ] REQ-STP-B-408: Cung cấp API summary hiệu quả gồm impression, click, CTR, add-to-cart và đơn hàng được gắn attribution.

#### Angular

- [ ] REQ-STP-F-401: Tạo danh sách/form campaign với lịch, trạng thái và promotion liên kết.
- [ ] REQ-STP-F-402: Tạo collection builder hỗ trợ tìm sản phẩm, thêm/xóa và kéo thả thay đổi thứ tự.
- [ ] REQ-STP-F-403: Tạo màn hình banner với preview desktop/mobile, alt text, target URL, vị trí và lịch hiển thị.
- [ ] REQ-STP-F-404: Tạo component công khai hiển thị campaign, collection và banner theo dữ liệu backend.
- [ ] REQ-STP-F-405: Gửi impression một lần cho mỗi lần hiển thị đủ điều kiện và gửi click trước khi điều hướng.
- [ ] REQ-STP-F-406: Hiển thị bảng summary hiệu quả campaign với bộ lọc thời gian.

#### Testing

- [ ] REQ-STP-T-401: Viết unit test cho lịch campaign/banner trước, trong và sau thời gian hiệu lực.
- [ ] REQ-STP-T-402: Viết integration test CRUD, publish/unpublish và API công khai chỉ trả nội dung hợp lệ.
- [ ] REQ-STP-T-403: Viết test reorder collection, thứ tự trùng và rollback khi cập nhật thất bại.
- [ ] REQ-STP-T-404: Viết test deduplication impression/click và phép tính CTR.
- [ ] REQ-STP-T-405: Viết Angular component test cho collection builder, banner preview và lịch validation.
- [ ] REQ-STP-T-406: Viết E2E test tạo campaign/collection/banner → publish → xem storefront → ghi nhận analytics.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **KhoaNXD1** | Các thành viên còn lại review khi có dependency | Toàn bộ requirement REQ-STP-B-401 → REQ-STP-B-408, REQ-STP-F-401 → REQ-STP-F-406 và REQ-STP-T-401 → REQ-STP-T-406 | Campaigns, Collections và Merchandising |

#### Workstream 3 — FEATURE-STP-03 — Personalized Recommendations & Evaluation

> Nguồn đối chiếu: Gói yêu cầu baseline số 6. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-03 — Personalized Recommendations & Evaluation.

**Mục tiêu tuần:** Bổ sung cá nhân hóa, fallback, khả năng giải thích và đo lường chất lượng recommendation.

#### Backend

- [ ] REQ-STP-B-601: Tạo hồ sơ sở thích suy luận theo user/session từ lịch sử xem, click, wishlist, add-to-cart và mua hàng.
- [ ] REQ-STP-B-602: Chỉ sử dụng dữ liệu cá nhân hóa khi có consent phù hợp; hỗ trợ xóa dữ liệu theo yêu cầu.
- [ ] REQ-STP-B-603: Tạo chiến lược personalized ranking kết hợp mức liên quan, độ mới, độ phổ biến và đa dạng danh mục.
- [ ] REQ-STP-B-604: Tạo fallback chain: personalized → context rule-based → best sellers → danh sách rỗng.
- [ ] REQ-STP-B-605: Cung cấp API recommendation theo placement, user/session, context product/category và giới hạn kết quả.
- [ ] REQ-STP-B-606: Trả metadata tối thiểu gồm strategy, request ID và reason code an toàn để quan sát/giải thích.
- [ ] REQ-STP-B-607: Tạo cơ chế chia variant A/B ổn định theo user/session và ghi variant vào sự kiện đo lường.
- [ ] REQ-STP-B-608: Tính metrics recommendation gồm CTR, add-to-cart rate, conversion rate và coverage theo placement/variant.

#### Angular

- [ ] REQ-STP-F-601: Tích hợp khối “Dành cho bạn” cho người dùng đủ điều kiện và fallback khi chưa đăng nhập.
- [ ] REQ-STP-F-602: Hiển thị nhãn giải thích ngắn như “Dựa trên sản phẩm bạn đã xem” khi reason code cho phép.
- [ ] REQ-STP-F-603: Gửi request ID, strategy và experiment variant trong impression/click event.
- [ ] REQ-STP-F-604: Tạo tùy chọn bật/tắt cá nhân hóa và liên kết đến phần quản lý quyền riêng tư.
- [ ] REQ-STP-F-605: Tạo dashboard quản trị so sánh CTR, add-to-cart và conversion theo placement/variant.
- [ ] REQ-STP-F-606: Không render recommendation cá nhân sau khi người dùng tắt consent hoặc yêu cầu xóa dữ liệu.

#### Testing

- [ ] REQ-STP-T-601: Viết unit test cho profile sở thích, ranking score, diversity và loại sản phẩm không hợp lệ.
- [ ] REQ-STP-T-602: Viết test fallback chain khi chưa đăng nhập, thiếu dữ liệu, strategy lỗi hoặc không có kết quả.
- [ ] REQ-STP-T-603: Viết privacy test cho consent on/off, xóa dữ liệu và không cá nhân hóa sau opt-out.
- [ ] REQ-STP-T-604: Viết test A/B assignment ổn định và metrics được phân nhóm đúng variant.
- [ ] REQ-STP-T-605: Viết offline evaluation tối thiểu Precision@K, Recall@K hoặc NDCG@K trên tập dữ liệu kiểm thử.
- [ ] REQ-STP-T-606: Viết E2E test user có lịch sử → nhận gợi ý cá nhân → tắt consent → nhận fallback không cá nhân hóa.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **HoangNQ17** | Các thành viên còn lại review khi có dependency | Toàn bộ requirement REQ-STP-B-601 → REQ-STP-B-608, REQ-STP-F-601 → REQ-STP-F-606 và REQ-STP-T-601 → REQ-STP-T-606 | Personalization, insight và hardening |

#### Workstream 4 — FEATURE-STP-04 — Analytics Dashboard, Export & Phase 1 Hardening

> Nguồn đối chiếu: Gói yêu cầu baseline số 8. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-04 — Analytics Dashboard, Export & Phase 1 Hardening.

**Mục tiêu tuần:** Hoàn thiện dashboard, xuất báo cáo, phân quyền, hiệu năng, bảo mật và nghiệm thu 4 feature cốt lõi của giai đoạn 1.

#### Backend

- [ ] REQ-STP-B-801: Cung cấp analytics overview gồm product views, add-to-cart rate, checkout rate, order conversion và returning customer rate.
- [ ] REQ-STP-B-802: Cung cấp product performance gồm lượt xem, wishlist, add-to-cart, order, rating và question count.
- [ ] REQ-STP-B-803: Cung cấp promotion/recommendation performance gồm impression, click, CTR, add-to-cart và attributed order.
- [ ] REQ-STP-B-804: Hỗ trợ lọc analytics theo khoảng ngày, danh mục, sản phẩm, campaign, placement và device type.
- [ ] REQ-STP-B-805: Tạo API xuất CSV bất đồng bộ, giới hạn khoảng dữ liệu và chỉ chứa aggregate không có PII.
- [ ] REQ-STP-B-806: Áp dụng quyền riêng cho xem dashboard, xem chi tiết và export; ghi audit log cho mọi lần export.
- [ ] REQ-STP-B-807: Cache hoặc pre-aggregate truy vấn nặng; định nghĩa thời điểm cập nhật dữ liệu và trả `lastUpdatedAt`.
- [ ] REQ-STP-B-808: Bổ sung rate limit, structured logging, metrics, health check và feature flags cho 4 feature cốt lõi.

#### Angular

- [ ] REQ-STP-F-801: Tạo analytics overview với KPI cards, conversion funnel và khoảng thời gian.
- [ ] REQ-STP-F-802: Tạo bảng product performance có tìm kiếm, lọc, sắp xếp và phân trang server-side.
- [ ] REQ-STP-F-803: Tạo dashboard promotion/recommendation với biểu đồ trend và so sánh campaign/placement.
- [ ] REQ-STP-F-804: Tạo chức năng export CSV hiển thị trạng thái đang tạo, hoàn thành, thất bại và link tải có thời hạn.
- [ ] REQ-STP-F-805: Chuẩn hóa loading, empty, stale, error state và hiển thị thời điểm cập nhật gần nhất.
- [ ] REQ-STP-F-806: Hoàn thiện responsive, keyboard navigation, label/description biểu đồ và route guard theo quyền.

#### Testing

- [ ] REQ-STP-T-801: Viết integration test các phép tính KPI, filter, attribution và dữ liệu không có sự kiện.
- [ ] REQ-STP-T-802: Viết authorization/privacy test cho dashboard, export, IDOR và xác nhận CSV không chứa PII.
- [ ] REQ-STP-T-803: Viết performance test cho event ingestion và analytics query với ngưỡng pass/fail được thống nhất.
- [ ] REQ-STP-T-804: Viết resilience test khi scheduled aggregation hoặc export job thất bại và chạy lại.
- [ ] REQ-STP-T-805: Viết E2E xuyên suốt review/Q&A → promotion → recommendation → customer journey → analytics.
- [ ] REQ-STP-T-806: Chạy regression, accessibility, security, migration và staging smoke test; không còn lỗi blocker/critical.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **TriTVV2** | Các thành viên còn lại review khi có dependency | Toàn bộ requirement REQ-STP-B-801 → REQ-STP-B-808, REQ-STP-F-801 → REQ-STP-F-806 và REQ-STP-T-801 → REQ-STP-T-806 | Dashboard, attribution và export |

### Week 3 — Bundle, Comparison & release hardening

**Mục tiêu tuần:** Hoàn thiện Smart Bundle Builder, Guided Bundle experience, Comparison Studio, fit guidance và nghiệm thu tích hợp toàn bộ 6 feature.

**Exit gate:** Bundle/Comparison chạy end-to-end; pricing và eligibility được backend xác thực; accessibility, security, performance, migration và staging smoke test đạt yêu cầu.

#### Workstream 1 — FEATURE-STP-05 — Bundle Model, Rules & Administration

> Nguồn đối chiếu: Gói yêu cầu baseline số 9. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-05 — Bundle Model, Rules & Administration.

**Mục tiêu tuần:** Hoàn thiện mô hình bundle, rule engine, API quản trị và giao diện tạo fixed/smart bundle.

#### Backend

- [ ] REQ-STP-B-901: Tạo `ProductBundle` gồm tên, slug, mô tả, loại `FIXED`/`SMART`, ảnh, trạng thái, thời gian hiệu lực và timestamps.
- [ ] REQ-STP-B-902: Tạo `BundleSlot` gồm tên slot, số lượng tối thiểu/tối đa, bắt buộc/tùy chọn, thứ tự và quy tắc chọn sản phẩm.
- [ ] REQ-STP-B-903: Hỗ trợ quy tắc slot theo sản phẩm, danh mục, thương hiệu, thuộc tính và khoảng giá bằng cấu trúc có thể mở rộng.
- [ ] REQ-STP-B-904: Tạo `BundleValidationService` kiểm tra đủ slot, số lượng, sản phẩm trùng, trạng thái hiển thị và tính tương thích.
- [ ] REQ-STP-B-905: Cung cấp CRUD, publish/unpublish, duplicate và API chi tiết bundle cho quản trị viên.
- [ ] REQ-STP-B-906: Cung cấp API công khai chỉ trả bundle đang hiệu lực và các thành phần đủ điều kiện hiển thị.
- [ ] REQ-STP-B-907: Tính tổng giá gốc, giá bundle và mức tiết kiệm ở backend; tích hợp promotion rule mà không tin giá do client gửi.
- [ ] REQ-STP-B-908: Ghi audit log khi tạo, sửa rule, thay đổi thành phần, publish hoặc unpublish bundle.

#### Angular

- [ ] REQ-STP-F-901: Tạo danh sách bundle có tìm kiếm, lọc theo loại/trạng thái và phân trang.
- [ ] REQ-STP-F-902: Tạo bundle wizard gồm thông tin chung, cấu hình slot, sản phẩm, giá và preview.
- [ ] REQ-STP-F-903: Tạo rule builder cho phép chọn sản phẩm, danh mục, thương hiệu, thuộc tính và khoảng giá theo từng slot.
- [ ] REQ-STP-F-904: Hỗ trợ kéo thả reorder slot/thành phần và hiển thị validation lỗi tại đúng vị trí.
- [ ] REQ-STP-F-905: Tạo preview storefront cho desktop/mobile với tổng giá và mức tiết kiệm.
- [ ] REQ-STP-F-906: Chỉ hiển thị publish khi cấu hình hợp lệ và yêu cầu xác nhận trước khi unpublish bundle đang hoạt động.

#### Testing

- [ ] REQ-STP-T-901: Viết unit test cho fixed/smart bundle, slot bắt buộc/tùy chọn, số lượng biên và sản phẩm trùng.
- [ ] REQ-STP-T-902: Viết test rule theo sản phẩm, danh mục, thương hiệu, thuộc tính, khoảng giá và nhiều điều kiện kết hợp.
- [ ] REQ-STP-T-903: Viết integration test CRUD, duplicate, publish/unpublish và API công khai theo thời gian hiệu lực.
- [ ] REQ-STP-T-904: Viết test tính giá gốc, giá bundle, promotion, làm tròn và client giả mạo giá.
- [ ] REQ-STP-T-905: Viết Angular component test cho wizard, rule builder, reorder, preview và validation.
- [ ] REQ-STP-T-906: Viết E2E test quản trị viên tạo smart bundle → cấu hình slot → preview → publish → xem trên storefront.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **GiangHV9** | **KhoaNXD1** | Toàn bộ requirement REQ-STP-B-901 → REQ-STP-B-908, REQ-STP-F-901 → REQ-STP-F-906 và REQ-STP-T-901 → REQ-STP-T-906 | Bundle data, rules và admin experience |

#### Workstream 2 — FEATURE-STP-05 — Guided Bundle Builder & Storefront Experience

> Nguồn đối chiếu: Gói yêu cầu baseline số 10. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-05 — Guided Bundle Builder & Storefront Experience.

**Mục tiêu tuần:** Hoàn thiện trải nghiệm tạo bundle theo nhu cầu/ngân sách, thay thế thành phần, lưu, chia sẻ và thêm toàn bộ vào giỏ.

#### Backend

- [ ] REQ-STP-B-1001: Tạo bundle builder session lưu template, user/session ID, câu trả lời nhu cầu, ngân sách, lựa chọn hiện tại và thời hạn.
- [ ] REQ-STP-B-1002: Cung cấp API câu hỏi hướng dẫn được cấu hình từ slot/thuộc tính và cho phép quản trị viên duyệt nội dung câu hỏi.
- [ ] REQ-STP-B-1003: Tạo `BundleCompositionService` chọn ứng viên cho từng slot theo rule, nhu cầu, ngân sách và recommendation score.
- [ ] REQ-STP-B-1004: Bảo đảm tổng bundle không vượt ngân sách khi có phương án hợp lệ; nếu không có phải trả trade-off/lý do rõ ràng.
- [ ] REQ-STP-B-1005: Cung cấp API sản phẩm thay thế theo slot, loại lựa chọn hiện tại và vẫn thỏa các ràng buộc bundle.
- [ ] REQ-STP-B-1006: Xác thực lại eligibility, giá và promotion của toàn bộ bundle trước khi thêm các thành phần vào giỏ trong một thao tác.
- [ ] REQ-STP-B-1007: Hỗ trợ lưu bundle vào tài khoản và chia sẻ bằng token ngẫu nhiên có thời hạn, không chứa user ID hoặc PII.
- [ ] REQ-STP-B-1008: Ghi sự kiện builder started/completed, answer selected, item replaced, bundle saved/shared và add-to-cart với request/session ID.

#### Angular

- [ ] REQ-STP-F-1001: Tạo guided bundle builder dạng từng bước với progress, câu hỏi, lựa chọn và khả năng quay lại.
- [ ] REQ-STP-F-1002: Hiển thị bundle được tạo theo slot, tổng giá, mức tiết kiệm, ngân sách còn lại/vượt và lý do lựa chọn.
- [ ] REQ-STP-F-1003: Cho phép thay thế từng sản phẩm bằng danh sách lựa chọn hợp lệ và cập nhật tổng giá ngay sau response backend.
- [ ] REQ-STP-F-1004: Hiển thị trade-off khi không thể thỏa đồng thời ngân sách và toàn bộ sở thích; không tuyên bố sai rằng có kết quả hoàn hảo.
- [ ] REQ-STP-F-1005: Bổ sung hành động lưu, chia sẻ, bắt đầu lại và thêm toàn bộ bundle vào giỏ; ngăn submit lặp.
- [ ] REQ-STP-F-1006: Đảm bảo builder responsive, điều hướng bàn phím, focus đúng bước và screen reader đọc được thay đổi giá.

#### Testing

- [ ] REQ-STP-T-1001: Viết unit test cho composition service với đủ/thiếu ứng viên, ngân sách thấp, nhiều phương án và rule xung đột.
- [ ] REQ-STP-T-1002: Viết test sản phẩm thay thế luôn đúng slot, không trùng và không phá điều kiện bắt buộc.
- [ ] REQ-STP-T-1003: Viết test giá thay đổi giữa lúc xây bundle và lúc thêm vào giỏ; backend phải trả kết quả cập nhật an toàn.
- [ ] REQ-STP-T-1004: Viết security/privacy test cho share token hết hạn, token đoán được, truy cập bundle riêng tư và PII leakage.
- [ ] REQ-STP-T-1005: Viết Angular component test cho stepper, replace item, trade-off, pricing update và accessibility.
- [ ] REQ-STP-T-1006: Viết E2E test trả lời nhu cầu → nhận bundle → thay sản phẩm → lưu/chia sẻ → thêm toàn bộ vào giỏ.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **KhoaNXD1** | **GiangHV9** | Toàn bộ requirement REQ-STP-B-1001 → REQ-STP-B-1008, REQ-STP-F-1001 → REQ-STP-F-1006 và REQ-STP-T-1001 → REQ-STP-T-1006 | Guided Bundle Builder và storefront experience |

#### Workstream 3 — FEATURE-STP-06 — Comparison Data, Normalization & Core UI

> Nguồn đối chiếu: Gói yêu cầu baseline số 11. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-06 — Comparison Data, Normalization & Core UI.

**Mục tiêu tuần:** Hoàn thiện mô hình thuộc tính so sánh, chuẩn hóa dữ liệu, API comparison và bảng so sánh cốt lõi.

#### Backend

- [ ] REQ-STP-B-1101: Tạo metadata thuộc tính so sánh gồm label, data type, unit, group, display order, comparable flag và importance.
- [ ] REQ-STP-B-1102: Tạo quy tắc chuẩn hóa giá trị và chuyển đổi đơn vị cho number, boolean, enum, text và measurement.
- [ ] REQ-STP-B-1103: Xác định compatibility group để chỉ cho so sánh trực tiếp các sản phẩm có bộ thuộc tính đủ tương thích.
- [ ] REQ-STP-B-1104: Cung cấp API comparison cho tối đa số sản phẩm cấu hình được; từ chối ID trùng, không tồn tại hoặc không đủ quyền hiển thị.
- [ ] REQ-STP-B-1105: Trả dữ liệu theo nhóm gồm thuộc tính chuẩn hóa, giá trị gốc, đơn vị, trạng thái thiếu dữ liệu và cờ khác biệt.
- [ ] REQ-STP-B-1106: Kết hợp giá hiện tại, promotion hợp lệ, rating summary và review count mà không làm thay đổi nguồn dữ liệu gốc.
- [ ] REQ-STP-B-1107: Hỗ trợ chọn variant trước khi so sánh và trả rõ thuộc tính/giá theo variant đã chọn.
- [ ] REQ-STP-B-1108: Hỗ trợ lưu comparison cho user và chia sẻ bằng token có thời hạn không chứa PII.

#### Angular

- [ ] REQ-STP-F-1101: Bổ sung nút “So sánh” trên product card và trang chi tiết; hiển thị số sản phẩm đang chọn.
- [ ] REQ-STP-F-1102: Tạo comparison tray cho phép xem nhanh, xóa sản phẩm, clear all và mở Comparison Studio.
- [ ] REQ-STP-F-1103: Tạo bảng so sánh sticky header/first column, nhóm thuộc tính và highlight các giá trị khác nhau.
- [ ] REQ-STP-F-1104: Cho phép bật “Chỉ xem điểm khác biệt”, chọn variant và xóa/thay sản phẩm ngay trong bảng.
- [ ] REQ-STP-F-1105: Hiển thị giá, promotion, rating, review count, dữ liệu thiếu và đơn vị một cách nhất quán.
- [ ] REQ-STP-F-1106: Tạo layout mobile theo từng thuộc tính/sản phẩm, hỗ trợ bàn phím và screen reader.

#### Testing

- [ ] REQ-STP-T-1101: Viết unit test chuẩn hóa number, enum, boolean, text, đơn vị tương đương và giá trị không hợp lệ.
- [ ] REQ-STP-T-1102: Viết integration test compatibility group, giới hạn số sản phẩm, ID trùng/ẩn/không tồn tại.
- [ ] REQ-STP-T-1103: Viết test cờ khác biệt với giá trị bằng nhau sau quy đổi, khác đơn vị và thiếu dữ liệu.
- [ ] REQ-STP-T-1104: Viết test variant-specific attributes, giá, promotion, rating summary và dữ liệu cập nhật.
- [ ] REQ-STP-T-1105: Viết Angular component test comparison tray/table, difference-only, variant và mobile layout.
- [ ] REQ-STP-T-1106: Viết E2E test chọn sản phẩm từ nhiều trang → mở comparison → đổi variant → lưu/chia sẻ.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **HoangNQ17** | **TriTVV2** | Toàn bộ requirement REQ-STP-B-1101 → REQ-STP-B-1108, REQ-STP-F-1101 → REQ-STP-F-1106 và REQ-STP-T-1101 → REQ-STP-T-1106 | Comparison data, normalization và core UI |

#### Workstream 4 — FEATURE-STP-06 — Comparison Insights, Fit Guidance & Final Hardening

> Nguồn đối chiếu: Gói yêu cầu baseline số 12. Mọi mã requirement được giữ nguyên; owner được chuẩn hóa theo mô hình một người sở hữu một feature và ghép cặp ở hai feature cuối.

**Feature:** FEATURE-STP-06 — Comparison Insights, Fit Guidance & Final Hardening.

**Mục tiêu tuần:** Bổ sung insight có thể giải thích, review digest, theo dõi hiệu quả và nghiệm thu tích hợp toàn bộ 6 feature.

#### Backend

- [ ] REQ-STP-B-1201: Tạo `ComparisonInsightService` xác định các khác biệt quan trọng dựa trên importance và ưu tiên người dùng.
- [ ] REQ-STP-B-1202: Hỗ trợ người dùng chọn tối đa số tiêu chí ưu tiên cấu hình được và tính fit score minh bạch theo trọng số.
- [ ] REQ-STP-B-1203: Trả reason codes và trade-off cho sản phẩm phù hợp nhất; không dùng nhãn tuyệt đối khi dữ liệu thiếu hoặc điểm gần nhau.
- [ ] REQ-STP-B-1204: Tích hợp review insight theo chủ đề vào comparison và luôn kèm review count/thời điểm tổng hợp.
- [ ] REQ-STP-B-1205: Khi sản phẩm không còn đủ điều kiện so sánh, đề xuất lựa chọn thay thế cùng compatibility group bằng recommendation service.
- [ ] REQ-STP-B-1206: Ghi sự kiện comparison created, product added/removed, priority changed, shared và product clicked.
- [ ] REQ-STP-B-1207: Cung cấp analytics comparison gồm sessions, completion, share rate, click-through và add-to-cart rate.
- [ ] REQ-STP-B-1208: Bổ sung cache, rate limit, structured logging, feature flag và tài liệu metric/runbook cho Bundle Builder và Comparison Studio.

#### Angular

- [ ] REQ-STP-F-1201: Tạo bộ chọn ưu tiên và cập nhật fit guidance sau response backend.
- [ ] REQ-STP-F-1202: Hiển thị “Phù hợp nhất với ưu tiên của bạn” kèm lý do, điểm và trade-off; không che các thuộc tính bất lợi.
- [ ] REQ-STP-F-1203: Hiển thị review insight theo chủ đề và liên kết mở các review nguồn tương ứng.
- [ ] REQ-STP-F-1204: Hiển thị lựa chọn thay thế khi một sản phẩm không còn khả dụng hoặc không tương thích.
- [ ] REQ-STP-F-1205: Bổ sung hành động lưu, chia sẻ, thêm sản phẩm vào wishlist/giỏ và tracking đầy đủ.
- [ ] REQ-STP-F-1206: Tạo dashboard comparison/bundle performance và hoàn thiện responsive/accessibility cho hai feature.

#### Testing

- [ ] REQ-STP-T-1201: Viết unit test fit score, trọng số ưu tiên, tie, dữ liệu thiếu và reason code/trade-off.
- [ ] REQ-STP-T-1202: Viết test review insight không bịa dữ liệu, xử lý ít review và liên kết đúng review nguồn.
- [ ] REQ-STP-T-1203: Viết test recommendation thay thế cùng compatibility group và loại sản phẩm không đủ điều kiện.
- [ ] REQ-STP-T-1204: Viết privacy/security/performance test cho share token, comparison API, builder API và analytics.
- [ ] REQ-STP-T-1205: Viết E2E xuyên suốt Guided Bundle Builder → Comparison Studio → wishlist/cart → analytics.
- [ ] REQ-STP-T-1206: Chạy regression, migration, accessibility và staging smoke test cho toàn bộ 6 feature; không còn lỗi blocker/critical.

#### Assignment

| Owner chính | Partner/reviewer | Requirement phụ trách | Trọng tâm |
| --- | --- | --- | --- |
| **TriTVV2** | **HoangNQ17** | Toàn bộ requirement REQ-STP-B-1201 → REQ-STP-B-1208, REQ-STP-F-1201 → REQ-STP-F-1206 và REQ-STP-T-1201 → REQ-STP-T-1206 | Comparison insights, fit guidance và release hardening |

## Cross-Feature Acceptance Criteria

- [ ] REQ-STP-X-001: API công khai chỉ trả nội dung, sản phẩm, bundle và comparison item đang đủ điều kiện hiển thị.
- [ ] REQ-STP-X-002: Backend luôn xác thực quyền, eligibility, promotion rule, bundle price, comparison data và aggregate; không tin cờ hoặc tổng số do client gửi.
- [ ] REQ-STP-X-003: Các thao tác vote, event ingestion, coupon usage, add bundle to cart và export job phải idempotent hoặc chống ghi trùng theo khóa nghiệp vụ.
- [ ] REQ-STP-X-004: Tất cả API danh sách hỗ trợ phân trang, giới hạn kích thước trang, sắp xếp ổn định và validation bộ lọc.
- [ ] REQ-STP-X-005: Mọi sự kiện analytics có schema/version, event ID, timestamp, source và consent state cần thiết.
- [ ] REQ-STP-X-006: Dữ liệu phân tích/export không chứa mật khẩu, token, địa chỉ đầy đủ hoặc PII không cần thiết.
- [ ] REQ-STP-X-007: UI mới đáp ứng responsive, keyboard navigation, focus state, form label và thông báo lỗi có thể đọc được.
- [ ] REQ-STP-X-008: Migration, API contract, event taxonomy, retention policy, dashboard metrics và runbook phải được tài liệu hóa trước release.

### Cross-Feature Assignment

| Thành viên | Requirement phụ trách | Vai trò điều phối |
| --- | --- | --- |
| **GiangHV9** | `REQ-STP-X-001` → `REQ-STP-X-002` | Nội dung hợp lệ và backend trust boundary |
| **KhoaNXD1** | `REQ-STP-X-003` → `REQ-STP-X-004` | Idempotency và API conventions |
| **HoangNQ17** | `REQ-STP-X-005` → `REQ-STP-X-006` | Event governance và data privacy |
| **TriTVV2** | `REQ-STP-X-007` → `REQ-STP-X-008` | Accessibility và release documentation |

> Mỗi thành viên là owner chính của **62 requirements**: 60 requirements trong 3 tuần (12 workstream) và 2 cross-feature requirements. Các thành viên khác vẫn tham gia review và hỗ trợ khi requirement giao nhau giữa Backend, Angular và Testing.

---

## Business Decisions Required

- [ ] DEC-STP-001: Trạng thái đơn nào làm khách đủ điều kiện `Verified Purchase`, và có giới hạn thời gian đánh giá không?
- [ ] DEC-STP-002: Người dùng được sửa/xóa đánh giá trong bao lâu và mỗi sản phẩm được phép có bao nhiêu đánh giá?
- [ ] DEC-STP-003: Ai được trả lời câu hỏi với tư cách chính thức và nội dung nào cần duyệt trước khi hiển thị?
- [ ] DEC-STP-004: Coupon có được cộng dồn với campaign hoặc coupon khác không; thứ tự áp dụng được xác định thế nào?
- [ ] DEC-STP-005: Lượt dùng coupon được ghi nhận khi tạo đơn hay ở một trạng thái đơn khác, và khi nào được hoàn lại?
- [ ] DEC-STP-006: Khoảng thời gian attribution cho campaign/recommendation là bao lâu và dùng last-click hay quy tắc khác?
- [ ] DEC-STP-007: Sự kiện nào cần consent, thời gian lưu raw event là bao lâu và quy trình xóa dữ liệu ra sao?
- [ ] DEC-STP-008: KPI chính và ngưỡng thành công cho review, promotion, recommendation và conversion funnel là gì?
- [ ] DEC-STP-009: Bundle hỗ trợ fixed, smart hay cả hai; mỗi bundle tối đa bao nhiêu slot và thành phần?
- [ ] DEC-STP-010: Giá bundle được tính từ tổng thành phần, giá cố định hay promotion riêng; có được cộng dồn coupon không?
- [ ] DEC-STP-011: Cho phép so sánh tối đa bao nhiêu sản phẩm và tiêu chí nào xác định sản phẩm tương thích?
- [ ] DEC-STP-012: Fit score dùng các trọng số mặc định nào và cách trình bày nào tránh tạo cảm giác hệ thống quyết định thay khách hàng?

### Decision Facilitation Assignment

| Thành viên | Decision điều phối | Trách nhiệm |
| --- | --- | --- |
| **GiangHV9** | `DEC-STP-001` → `DEC-STP-002`; `DEC-STP-009` | Eligibility/review lifecycle và phạm vi bundle |
| **KhoaNXD1** | `DEC-STP-003` → `DEC-STP-004`; `DEC-STP-010` | Q&A, promotion stacking và bundle pricing |
| **HoangNQ17** | `DEC-STP-005` → `DEC-STP-006`; `DEC-STP-011` | Coupon lifecycle, attribution và giới hạn comparison |
| **TriTVV2** | `DEC-STP-007` → `DEC-STP-008`; `DEC-STP-012` | Privacy/retention, KPI và fit score presentation |

> Người điều phối chuẩn bị phương án và ghi nhận quyết định; quyết định nghiệp vụ cuối cùng thuộc Product Owner/Team Lead.

---

## Definition of Done

Một feature được xem là hoàn thành khi:

- Toàn bộ requirement thuộc phạm vi release đã hoàn thành hoặc có phê duyệt loại khỏi phạm vi.
- Code đã được review và không còn nhận xét bắt buộc xử lý.
- Unit, integration, component và E2E test liên quan chạy thành công trên CI.
- Không còn lỗi blocker/critical; lỗi còn lại có owner, mức độ và kế hoạch xử lý.
- Migration được kiểm tra từ database trống và từ phiên bản đang triển khai mà không mất dữ liệu.
- Phân quyền, audit log, analytics, monitoring và feature flag liên quan đã sẵn sàng.
- Accessibility, privacy, retention và security criteria đã được kiểm tra.
- API contract, event taxonomy, metric definition và tài liệu vận hành đã được cập nhật.
- Product Owner/Team Lead đã nghiệm thu acceptance criteria và quyết định nghiệp vụ liên quan.

