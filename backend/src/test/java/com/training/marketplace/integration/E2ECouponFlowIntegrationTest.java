package com.training.marketplace.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.dto.request.LoginRequest;
import com.training.marketplace.dto.request.RegisterRequest;
import com.training.marketplace.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REQ-STP-T-306: full coupon E2E — admin creates a coupon, a customer applies it (preview),
 * changes the cart (backend re-checks and updates totals), then checks out and the order keeps the
 * correct server-computed discount snapshot. Uses seeded admin (admin@marketplace.com/admin123) and
 * seeded variant 5 (price 399.99, 50 units in the default warehouse 1).
 */
class E2ECouponFlowIntegrationTest extends BaseIntegrationTest {

    private static final long VARIANT_ID = 5;      // WH1000XM5-BLK, price 399.99
    private static final String COUPON = "E2E10";  // PERCENT 10%, whole-cart

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void adminCreatesCoupon_customerApplies_changesCart_checkoutKeepsSnapshot() throws Exception {
        // Admin logs in and creates a 10% whole-cart coupon.
        String adminToken = token(login("admin@marketplace.com", "admin123"));
        Map<String, Object> couponBody = Map.of(
                "code", COUPON, "discountType", "PERCENT", "discountValue", 10,
                "minOrderAmount", 0, "scopeType", "CART", "active", true);
        ResponseEntity<String> created = exchange("/api/v1/coupons", HttpMethod.POST, couponBody, adminToken);
        assertThat(created.getStatusCode().is2xxSuccessful()).isTrue();

        // Customer registers (register returns a token).
        AuthResponse reg = restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest("e2ecoupon", "e2ecoupon@example.com", "Password123!", "E2E Coupon"),
                AuthResponse.class).getBody();
        assertThat(reg).isNotNull();
        String custToken = reg.accessToken();

        // Add 2 units of variant 5 -> subtotal 799.98.
        exchange("/api/v1/cart/items", HttpMethod.POST, Map.of("variantId", VARIANT_ID, "quantity", 2), custToken);

        // Preview the coupon: 10% of 799.98 = 80.00, new total 719.98.
        JsonNode p1 = data(exchange("/api/v1/coupons/preview", HttpMethod.POST, Map.of("code", COUPON), custToken));
        assertThat(p1.path("valid").asBoolean()).isTrue();
        assertThat(dec(p1, "cartSubtotal")).isEqualByComparingTo("799.98");
        assertThat(dec(p1, "discountAmount")).isEqualByComparingTo("80.00");
        assertThat(dec(p1, "newTotal")).isEqualByComparingTo("719.98");

        // Change the cart to 3 units -> subtotal 1199.97; backend re-checks the coupon (F-306).
        exchange("/api/v1/cart/items/" + VARIANT_ID, HttpMethod.PUT, Map.of("quantity", 3), custToken);
        JsonNode p2 = data(exchange("/api/v1/coupons/preview", HttpMethod.POST, Map.of("code", COUPON), custToken));
        assertThat(dec(p2, "discountAmount")).isEqualByComparingTo("120.00");
        assertThat(dec(p2, "newTotal")).isEqualByComparingTo("1079.97");

        // Checkout with the coupon: the order stores the server-computed snapshot.
        ResponseEntity<String> orderResp = exchange("/api/v1/orders", HttpMethod.POST,
                Map.of("shippingAddress", "123 E2E Street", "couponCode", COUPON), custToken);
        assertThat(orderResp.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode order = data(orderResp);
        assertThat(order.path("couponCode").asText()).isEqualTo(COUPON);
        assertThat(dec(order, "discountAmount")).isEqualByComparingTo("120.00");
        assertThat(dec(order, "totalAmount")).isEqualByComparingTo("1079.97"); // 1199.97 - 120.00
    }

    // ---- helpers

    private AuthResponse login(String email, String password) {
        return restTemplate.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, password), AuthResponse.class).getBody();
    }

    private String token(AuthResponse auth) {
        assertThat(auth).isNotNull();
        return auth.accessToken();
    }

    private ResponseEntity<String> exchange(String url, HttpMethod method, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode data(ResponseEntity<String> resp) throws Exception {
        return mapper.readTree(resp.getBody()).path("data");
    }

    private BigDecimal dec(JsonNode node, String field) {
        return new BigDecimal(node.path(field).asText());
    }
}
