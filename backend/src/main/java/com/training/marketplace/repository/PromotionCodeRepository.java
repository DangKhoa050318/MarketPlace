package com.training.marketplace.repository;

import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.enums.DiscountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionCodeRepository extends JpaRepository<PromotionCode, Long> {

    Optional<PromotionCode> findByCode(String code);

    boolean existsByCode(String code);

    /** Locks the coupon row so the used_count check-and-increment is atomic under concurrency (B-308). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromotionCode p WHERE p.code = :code")
    Optional<PromotionCode> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromotionCode p WHERE p.id = :id")
    Optional<PromotionCode> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT p FROM PromotionCode p
            WHERE (:active IS NULL OR p.active = :active)
              AND (:type IS NULL OR p.discountType = :type)
              AND (:q IS NULL OR UPPER(p.code) LIKE UPPER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<PromotionCode> search(
            @Param("active") Boolean active,
            @Param("type") DiscountType type,
            @Param("q") String q,
            Pageable pageable);
}
