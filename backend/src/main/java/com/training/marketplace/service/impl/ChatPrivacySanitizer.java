package com.training.marketplace.service.impl;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ChatPrivacySanitizer {

    private static final Pattern EMAIL = Pattern.compile(
            "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern VIETNAMESE_PHONE = Pattern.compile(
            "(?<!\\d)(?:\\+?84|0)(?:[ .-]?\\d){9,10}(?!\\d)");
    private static final Pattern TOKEN = Pattern.compile(
            "(?i)\\b(?:bearer|token|jwt|api[_ -]?key)\\s*[:=]?\\s*[A-Za-z0-9._~-]{12,}");

    public String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = EMAIL.matcher(value).replaceAll("[email]");
        sanitized = VIETNAMESE_PHONE.matcher(sanitized).replaceAll("[phone]");
        return TOKEN.matcher(sanitized).replaceAll("[secret]");
    }
}
