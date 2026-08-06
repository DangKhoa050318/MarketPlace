package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.ChatbotRequest;
import com.training.marketplace.dto.response.ChatbotResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.PromotionCodeRepository;
import com.training.marketplace.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private final ProductRepository productRepository;
    private final PromotionCodeRepository promotionCodeRepository;
    private final RestClient.Builder restClientBuilder;

    @Value("${marketplace.ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Override
    public ChatbotResponse askQuestion(ChatbotRequest request) {
        // 1. Gather context from Database
        String productContext = getActiveProductsContext();
        String couponContext = getActiveCouponsContext();

        // 2. Prepare Payload for Python AI Service
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", request.getMessage());
        payload.put("product_context", productContext);
        payload.put("coupon_context", couponContext);
        payload.put("chat_history", request.getChatHistory());

        // 3. Make HTTP request to Python AI Service
        try {
            RestClient restClient = restClientBuilder.baseUrl(aiServiceUrl).build();
            ChatbotResponse response = restClient.post()
                    .uri("/api/chat")
                    .body(payload)
                    .retrieve()
                    .body(ChatbotResponse.class);
            return response;
        } catch (Exception e) {
            log.error("Error communicating with AI Service: {}", e.getMessage(), e);
            throw new RuntimeException("Xin lỗi, hệ thống Chatbot hiện đang gặp sự cố. Vui lòng thử lại sau.");
        }
    }

    private String getActiveProductsContext() {
        // Fetch up to 20 active products for context to avoid huge payloads
        Page<Product> products = productRepository.searchProducts("", PageRequest.of(0, 20));
        if (products.isEmpty()) return "Không có sản phẩm nào.";

        return products.stream()
                .map(p -> String.format("- %s (Brand: %s): %s", p.getName(), p.getBrand(), p.getDescription()))
                .collect(Collectors.joining("\n"));
    }

    private String getActiveCouponsContext() {
        // Fetch active coupons
        Page<PromotionCode> coupons = promotionCodeRepository.search(true, null, null, PageRequest.of(0, 10));
        if (coupons.isEmpty()) return "Không có mã giảm giá nào đang hiệu lực.";

        return coupons.stream()
                .map(c -> String.format("- Mã: %s (Giảm: %s, Loại: %s) Hết hạn: %s", 
                        c.getCode(), c.getDiscountValue(), c.getDiscountType(), c.getExpiresAt()))
                .collect(Collectors.joining("\n"));
    }
}
