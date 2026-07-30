package com.training.marketplace.repository;

import com.training.marketplace.entity.AnonymousWishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnonymousWishlistItemRepository extends JpaRepository<AnonymousWishlistItem, Long> {

    Optional<AnonymousWishlistItem> findBySessionIdAndProductId(String sessionId, Long productId);

    boolean existsBySessionIdAndProductId(String sessionId, Long productId);

    void deleteBySessionIdAndProductId(String sessionId, Long productId);

    void deleteBySessionId(String sessionId);

    @Modifying
    @Query(value = """
            INSERT INTO wishlist_items (user_id, product_id, created_at, updated_at)
            SELECT :userId, anonymous.product_id, MIN(anonymous.created_at), NOW()
              FROM anonymous_wishlist_items anonymous
             WHERE anonymous.session_id = :sessionId
             GROUP BY anonymous.product_id
            ON CONFLICT (user_id, product_id) DO NOTHING
            """, nativeQuery = true)
    int mergeSessionIntoUser(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}
