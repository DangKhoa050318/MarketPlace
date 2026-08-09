package com.training.marketplace.service;

import java.util.Set;

public record ChatPreferenceProfile(
        Set<Long> categoryIds,
        Set<String> brands
) {
    public ChatPreferenceProfile {
        categoryIds = categoryIds == null ? Set.of() : Set.copyOf(categoryIds);
        brands = brands == null ? Set.of() : Set.copyOf(brands);
    }

    public static ChatPreferenceProfile empty() {
        return new ChatPreferenceProfile(Set.of(), Set.of());
    }
}
