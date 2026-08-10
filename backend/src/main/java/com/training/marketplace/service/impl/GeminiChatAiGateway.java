package com.training.marketplace.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.config.ChatAssistantProperties;
import com.training.marketplace.dto.request.ChatPageContext;
import com.training.marketplace.enums.ChatIntent;
import com.training.marketplace.service.ChatAiGateway;
import com.training.marketplace.service.ChatHistoryMessage;
import com.training.marketplace.service.ChatIntentAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiChatAiGateway implements ChatAiGateway {

    private final ObjectMapper objectMapper;
    private final ChatAssistantProperties properties;
    private final ChatPrivacySanitizer privacySanitizer;

    @Override
    public Optional<ChatIntentAnalysis> analyze(
            String message,
            List<ChatHistoryMessage> history,
            ChatPageContext pageContext) {
        ChatAssistantProperties.Ai ai = properties.getAi();
        if (!properties.isEnabled() || !ai.isEnabled()
                || ai.getApiKey() == null || ai.getApiKey().isBlank()) {
            return Optional.empty();
        }
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(ai.getConnectTimeout());
            requestFactory.setReadTimeout(ai.getReadTimeout());
            RestClient client = RestClient.builder()
                    .baseUrl(ai.getBaseUrl())
                    .requestFactory(requestFactory)
                    .build();
            JsonNode response = client.post()
                    .uri("/models/{model}:generateContent", ai.getModel())
                    .header("x-goog-api-key", ai.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload(prompt(message, history, pageContext)))
                    .retrieve()
                    .body(JsonNode.class);
            return parse(response);
        } catch (RuntimeException exception) {
            log.warn("Gemini intent analysis failed; using deterministic fallback: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> payload(String prompt) {
        return Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "responseMimeType", "application/json"));
    }

    private String prompt(
            String message,
            List<ChatHistoryMessage> history,
            ChatPageContext pageContext) {
        StringBuilder prompt = new StringBuilder("""
                Bạn là bộ phân tích ý định mua sắm cho MarketPlace. Chỉ trả JSON hợp lệ, không Markdown.
                Không tự tạo tên sản phẩm, giá, tồn kho hoặc voucher.
                Các intent hợp lệ: GREETING, THANKS, HELP, BEST_SELLER, PRODUCT_DISCOVERY,
                CAMPAIGN_OFFERS, CHECKOUT. Chỉ dùng GREETING, THANKS hoặc HELP khi câu nói thuần hội thoại
                và không chứa nhu cầu mua sắm.
                Trích xuất ràng buộc từ cả lịch sử và câu mới. Giá luôn đổi sang VND dạng số.
                Schema: {"intents":["PRODUCT_DISCOVERY"],"query":null,"category":null,
                "minPrice":null,"maxPrice":null,"brand":null,"attributes":{},
                "needsClarification":false,"clarificationQuestion":null}
                """);
        if (history != null && !history.isEmpty()) {
            prompt.append("\nLịch sử gần đây:\n");
            history.stream().skip(Math.max(0, history.size() - 6)).forEach(item -> prompt
                    .append(item.role()).append(": ")
                    .append(privacySanitizer.sanitize(item.content())).append('\n'));
        }
        if (pageContext != null) {
            prompt.append("Ngữ cảnh trang: productId=").append(pageContext.productId())
                    .append(", categoryId=").append(pageContext.categoryId()).append('\n');
        }
        prompt.append("Câu mới: ").append(privacySanitizer.sanitize(message));
        return prompt.toString();
    }

    private Optional<ChatIntentAnalysis> parse(JsonNode response) {
        JsonNode textNode = response == null ? null
                : response.at("/candidates/0/content/parts/0/text");
        if (textNode == null || textNode.isMissingNode() || textNode.asText().isBlank()) {
            return Optional.empty();
        }
        try {
            String json = stripCodeFence(textNode.asText());
            JsonNode node = objectMapper.readTree(json);
            List<ChatIntent> intents = new ArrayList<>();
            node.path("intents").forEach(item -> {
                try {
                    intents.add(ChatIntent.valueOf(item.asText()));
                } catch (IllegalArgumentException ignored) {
                    // Ignore provider values outside the declared schema.
                }
            });
            Map<String, String> attributes = new LinkedHashMap<>();
            node.path("attributes").fields().forEachRemaining(entry ->
                    attributes.put(entry.getKey(), entry.getValue().asText()));
            return Optional.of(new ChatIntentAnalysis(
                    intents,
                    text(node, "query"),
                    text(node, "category"),
                    decimal(node, "minPrice"),
                    decimal(node, "maxPrice"),
                    text(node, "brand"),
                    attributes,
                    node.path("needsClarification").asBoolean(false),
                    text(node, "clarificationQuestion")));
        } catch (JsonProcessingException exception) {
            log.warn("Gemini returned invalid intent JSON; using fallback");
            return Optional.empty();
        }
    }

    private String stripCodeFence(String value) {
        String stripped = value.trim();
        if (stripped.startsWith("```")) {
            stripped = stripped.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        return stripped;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText().trim();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isNumber()) {
            return null;
        }
        return value.decimalValue();
    }
}
