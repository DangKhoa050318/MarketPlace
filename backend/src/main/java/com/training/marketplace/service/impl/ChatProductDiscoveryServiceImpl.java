package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.dto.response.RecommendationItemResponse;
import com.training.marketplace.repository.ChatPreferenceQueryRepository;
import com.training.marketplace.repository.ChatProductQueryRepository;
import com.training.marketplace.repository.CategoryRepository;
import com.training.marketplace.service.ChatPreferenceProfile;
import com.training.marketplace.service.ChatProductCandidate;
import com.training.marketplace.service.ChatProductDiscoveryService;
import com.training.marketplace.service.ChatSearchCriteria;
import com.training.marketplace.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.Normalizer;

@Service
@RequiredArgsConstructor
public class ChatProductDiscoveryServiceImpl implements ChatProductDiscoveryService {

    private static final int RECOMMENDATION_MAX_LIMIT = 24;

    private final ChatProductQueryRepository productQueryRepository;
    private final ChatPreferenceQueryRepository preferenceQueryRepository;
    private final RecommendationService recommendationService;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChatProductCandidate> discover(
            ChatSearchCriteria criteria,
            boolean bestSeller,
            Long userId,
            String sessionId) {
        ChatPreferenceProfile preferences = loadPreferences(userId, sessionId);
        List<ChatProductCandidate> candidates;
        Map<Long, Integer> bestSellerRanks = Map.of();

        if (bestSeller) {
            Long categoryId = resolveCategoryId(criteria);
            RecommendationPlacement placement = categoryId == null
                    ? RecommendationPlacement.HOME_BEST_SELLERS
                    : RecommendationPlacement.CATEGORY_BEST_SELLERS;
            int recommendationLimit = Math.min(
                    RECOMMENDATION_MAX_LIMIT,
                    Math.max(criteria.limit() * 4, criteria.limit()));
            var recommendation = recommendationService.recommend(
                    userId,
                    sessionId,
                    placement,
                    null,
                    categoryId,
                    recommendationLimit);
            List<Long> ids = recommendation.items().stream()
                    .map(RecommendationItemResponse::product)
                    .map(product -> product.id())
                    .toList();
            bestSellerRanks = new HashMap<>();
            for (int i = 0; i < ids.size(); i++) {
                bestSellerRanks.put(ids.get(i), i);
            }
            candidates = productQueryRepository.findByProductIds(ids, criteria);
        } else {
            candidates = productQueryRepository.search(criteria);
            if (candidates.isEmpty() && hasText(criteria.query())) {
                Long categoryId = resolveCategoryId(criteria);
                if (categoryId != null) {
                    candidates = productQueryRepository.search(relaxNeeds(criteria, categoryId));
                }
            }
        }

        List<ChatProductCandidate> ranked = new ArrayList<>(candidates);
        Map<Long, Integer> immutableRanks = bestSellerRanks;
        ranked.sort(Comparator
                .comparingInt((ChatProductCandidate candidate) -> score(
                        candidate, preferences, immutableRanks, bestSeller)).reversed()
                .thenComparing(ChatProductCandidate::productId));
        return ranked.stream().limit(criteria.limit()).toList();
    }

    private int score(
            ChatProductCandidate candidate,
            ChatPreferenceProfile preferences,
            Map<Long, Integer> bestSellerRanks,
            boolean bestSeller) {
        int score = 0;
        if (preferences.categoryIds().contains(candidate.categoryId())) {
            score += 20;
        }
        if (candidate.brand() != null && preferences.brands().stream()
                .anyMatch(brand -> brand.equalsIgnoreCase(candidate.brand()))) {
            score += 10;
        }
        if (bestSeller) {
            Integer rank = bestSellerRanks.get(candidate.productId());
            score += rank == null ? 0 : 1000 - rank * 20;
        }
        score += (int) Math.min(candidate.availableStock(), 9);
        return score;
    }

    private ChatPreferenceProfile loadPreferences(Long userId, String sessionId) {
        try {
            return preferenceQueryRepository.load(userId, sessionId);
        } catch (RuntimeException ignored) {
            return ChatPreferenceProfile.empty();
        }
    }

    private Long resolveCategoryId(ChatSearchCriteria criteria) {
        if (criteria.pageCategoryId() != null) {
            return criteria.pageCategoryId();
        }
        String requestedCategory = hasText(criteria.category())
                ? criteria.category() : criteria.query();
        if (!hasText(requestedCategory)) {
            return null;
        }
        return categoryRepository.findAll().stream()
                .filter(category -> matchesCategory(requestedCategory, category.getName()))
                .map(category -> category.getId())
                .findFirst()
                .orElse(null);
    }

    private ChatSearchCriteria relaxNeeds(ChatSearchCriteria criteria, Long categoryId) {
        return new ChatSearchCriteria(
                null,
                null,
                categoryId,
                criteria.minPrice(),
                criteria.maxPrice(),
                criteria.brand(),
                criteria.productScopeIds(),
                criteria.categoryScopeIds(),
                criteria.offerOnly(),
                criteria.cartWideOffer(),
                criteria.limit());
    }

    private boolean matchesCategory(String requested, String categoryName) {
        String normalizedRequest = normalize(requested);
        String normalizedCategory = normalize(categoryName);
        if (normalizedCategory.contains(normalizedRequest)
                || normalizedRequest.contains(normalizedCategory)) {
            return true;
        }
        List<String> requestedTokens = tokens(normalizedRequest);
        List<String> categoryTokens = tokens(normalizedCategory);
        return requestedTokens.stream().anyMatch(requestedToken -> categoryTokens.stream()
                .anyMatch(categoryToken -> requestedToken.startsWith(categoryToken)
                        || categoryToken.startsWith(requestedToken)));
    }

    private List<String> tokens(String value) {
        return java.util.Arrays.stream(value.split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 4)
                .toList();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
