package com.training.marketplace.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the chat privacy scrubber (MP-L4). It redacts PII/secrets before user text is folded
 * into the LLM prompt, so the key behaviours are: emails, Vietnamese phone numbers and bearer/api-key
 * tokens get masked, while ordinary shopping text — crucially price numbers — is left untouched.
 */
class ChatPrivacySanitizerTest {

    private final ChatPrivacySanitizer sanitizer = new ChatPrivacySanitizer();

    @Test
    void nullOrBlank_returnsEmptyString() {
        assertThat(sanitizer.sanitize(null)).isEmpty();
        assertThat(sanitizer.sanitize("   ")).isEmpty();
    }

    @Test
    void redactsEmailAddress() {
        String result = sanitizer.sanitize("Liên hệ khoa.nxd@gmail.com giúp mình");
        assertThat(result).contains("[email]").doesNotContain("khoa.nxd@gmail.com");
    }

    @Test
    void redactsVietnamesePhoneNumbers() {
        assertThat(sanitizer.sanitize("SĐT của tôi 0912345678")).contains("[phone]").doesNotContain("0912345678");
        assertThat(sanitizer.sanitize("gọi +84912345678 nhé")).contains("[phone]").doesNotContain("84912345678");
    }

    @Test
    void redactsBearerTokenAndApiKey() {
        assertThat(sanitizer.sanitize("token: Bearer abcdef1234567890xyz")).contains("[secret]");
        assertThat(sanitizer.sanitize("api_key=ABCDEF123456ghijklmnop")).contains("[secret]");
    }

    @Test
    void leavesOrdinaryShoppingTextUntouched_includingPrices() {
        String text = "Tìm áo thun nam dưới 200000 màu đen size L";
        assertThat(sanitizer.sanitize(text)).isEqualTo(text);
    }

    @Test
    void redactsMultiplePiiInOneMessage() {
        String result = sanitizer.sanitize("email a.b@shop.vn, sđt 0987654321, tìm giày dưới 500000");
        assertThat(result)
                .contains("[email]")
                .contains("[phone]")
                .contains("500000")
                .doesNotContain("a.b@shop.vn")
                .doesNotContain("0987654321");
    }
}
