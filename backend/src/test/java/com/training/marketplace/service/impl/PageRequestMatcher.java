package com.training.marketplace.service.impl;

import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Pageable;

final class PageRequestMatcher {

    private PageRequestMatcher() {
    }

    static Pageable pageSize(int expectedSize) {
        return ArgumentMatchers.argThat(
                (ArgumentMatcher<Pageable>) pageable ->
                        pageable != null && pageable.getPageSize() == expectedSize);
    }
}
