package com.training.marketplace.service;

import java.time.Duration;

/** Server-side state for the single active refresh-token session of each user. */
public interface RefreshTokenStore {

    void save(String username, String tokenId, Duration ttl);

    boolean rotate(String username, String currentTokenId, String newTokenId, Duration ttl);

    void revoke(String username, String tokenId);
}
