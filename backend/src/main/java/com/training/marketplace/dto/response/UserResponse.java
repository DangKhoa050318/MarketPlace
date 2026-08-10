package com.training.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        boolean active,
        BigDecimal walletBalance,
        LocalDateTime createdAt
) {
    public UserResponse(
            Long id,
            String username,
            String email,
            String fullName,
            String role,
            boolean active,
            LocalDateTime createdAt
    ) {
        this(id, username, email, fullName, role, active, BigDecimal.ZERO, createdAt);
    }
}
