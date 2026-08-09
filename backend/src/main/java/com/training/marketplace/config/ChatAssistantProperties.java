package com.training.marketplace.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "marketplace.chat")
public class ChatAssistantProperties {

    private boolean enabled = true;

    @NotNull
    private Duration conversationTtl = Duration.ofHours(24);

    @Min(1)
    @Max(12)
    private int maxProducts = 6;

    @Min(2)
    @Max(30)
    private int maxHistoryMessages = 12;

    private final Ai ai = new Ai();

    @Getter
    @Setter
    public static class Ai {
        private boolean enabled = true;
        private String apiKey;
        private String model = "gemini-2.5-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration readTimeout = Duration.ofSeconds(8);
    }
}
