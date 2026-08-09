package com.training.marketplace.service.impl;

import com.training.marketplace.enums.ChatIntent;
import com.training.marketplace.service.ChatHistoryMessage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedChatIntentAnalyzerTest {

    private final RuleBasedChatIntentAnalyzer analyzer = new RuleBasedChatIntentAnalyzer();

    @Test
    void recognizesPureGreetingWithoutStartingProductDiscovery() {
        var result = analyzer.analyze("Xin chào bạn", List.of(), null);

        assertThat(result.intents()).containsExactly(ChatIntent.GREETING);
        assertThat(result.query()).isNull();
        assertThat(result.needsClarification()).isFalse();
    }

    @Test
    void recognizesThanksAndHelpAsConversationalIntents() {
        var thanks = analyzer.analyze("Cảm ơn bạn nhiều nhé", List.of(), null);
        var help = analyzer.analyze("Bạn có thể giúp gì?", List.of(), null);

        assertThat(thanks.intents()).containsExactly(ChatIntent.THANKS);
        assertThat(help.intents()).containsExactly(ChatIntent.HELP);
    }

    @Test
    void keepsShoppingNeedWhenGreetingAndRequestAreCombined() {
        var result = analyzer.analyze("Xin chào, gợi ý laptop dưới 20 triệu", List.of(), null);

        assertThat(result.intents()).containsExactly(ChatIntent.PRODUCT_DISCOVERY);
        assertThat(result.query()).isEqualTo("laptop");
        assertThat(result.maxPrice()).isEqualByComparingTo(new BigDecimal("20000000"));
    }

    @Test
    void extractsCombinedIntentProductAndVietnameseBudget() {
        var result = analyzer.analyze(
                "Gợi ý laptop bán chạy dưới 20 triệu đang có voucher",
                List.of(),
                null);

        assertThat(result.intents()).containsExactly(
                ChatIntent.BEST_SELLER,
                ChatIntent.CAMPAIGN_OFFERS,
                ChatIntent.PRODUCT_DISCOVERY);
        assertThat(result.maxPrice()).isEqualByComparingTo(new BigDecimal("20000000"));
        assertThat(result.query()).isEqualTo("laptop");
        assertThat(result.needsClarification()).isFalse();
    }

    @Test
    void usesRecentUserHistoryForFollowUpConstraints() {
        var result = analyzer.analyze(
                "Dưới 15 triệu",
                List.of(new ChatHistoryMessage("user", "Mình cần laptop")),
                null);

        assertThat(result.maxPrice()).isEqualByComparingTo(new BigDecimal("15000000"));
        assertThat(result.needsClarification()).isFalse();
    }

    @Test
    void asksForClarificationWhenNoShoppingNeedIsPresent() {
        var result = analyzer.analyze("Gợi ý cho mình", List.of(), null);

        assertThat(result.needsClarification()).isTrue();
        assertThat(result.clarificationQuestion()).contains("ngân sách");
    }
}
