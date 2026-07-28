package com.training.marketplace.repository;

import com.training.marketplace.entity.RecentlyViewedProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecentlyViewedProductRepository extends JpaRepository<RecentlyViewedProduct, Long> {

    Optional<RecentlyViewedProduct> findByUserIdAndProductId(Long userId, Long productId);

    Optional<RecentlyViewedProduct> findBySessionIdAndProductId(String sessionId, Long productId);

    @Query("""
            SELECT rvp FROM RecentlyViewedProduct rvp
            WHERE rvp.userId = :userId
            ORDER BY rvp.viewedAt DESC, rvp.id DESC
            """)
    List<RecentlyViewedProduct> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT rvp FROM RecentlyViewedProduct rvp
            WHERE rvp.sessionId = :sessionId
            ORDER BY rvp.viewedAt DESC, rvp.id DESC
            """)
    List<RecentlyViewedProduct> findRecentBySessionId(@Param("sessionId") String sessionId, Pageable pageable);

    @Modifying
    @Query(value = """
            DELETE FROM recently_viewed_products
            WHERE user_id = :userId
              AND id NOT IN (
                  SELECT id FROM recently_viewed_products
                  WHERE user_id = :userId
                  ORDER BY viewed_at DESC, id DESC
                  LIMIT :limit
              )
            """, nativeQuery = true)
    void deleteUserOverflow(@Param("userId") Long userId, @Param("limit") int limit);

    @Modifying
    @Query(value = """
            DELETE FROM recently_viewed_products
            WHERE session_id = :sessionId
              AND id NOT IN (
                  SELECT id FROM recently_viewed_products
                  WHERE session_id = :sessionId
                  ORDER BY viewed_at DESC, id DESC
                  LIMIT :limit
              )
            """, nativeQuery = true)
    void deleteSessionOverflow(@Param("sessionId") String sessionId, @Param("limit") int limit);

    void deleteByUserId(Long userId);

    void deleteBySessionId(String sessionId);
}
