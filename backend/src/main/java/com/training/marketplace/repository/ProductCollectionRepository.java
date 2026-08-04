package com.training.marketplace.repository;

import com.training.marketplace.entity.ProductCollection;
import com.training.marketplace.enums.PublishStatus;
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
public interface ProductCollectionRepository extends JpaRepository<ProductCollection, Long> {

    Optional<ProductCollection> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT c FROM ProductCollection c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:q IS NULL OR UPPER(c.name) LIKE UPPER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<ProductCollection> search(@Param("status") PublishStatus status,
                                   @Param("q") String q,
                                   Pageable pageable);

    /** Lock the collection row so a full item reorder rewrites positions atomically (B-406). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ProductCollection c WHERE c.id = :id")
    Optional<ProductCollection> findByIdForUpdate(@Param("id") Long id);
}
