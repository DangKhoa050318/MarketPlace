package com.training.marketplace.repository;

import com.training.marketplace.entity.ProductCollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCollectionItemRepository extends JpaRepository<ProductCollectionItem, Long> {

    List<ProductCollectionItem> findByCollectionIdOrderByDisplayOrderAsc(Long collectionId);

    Optional<ProductCollectionItem> findByCollectionIdAndProductId(Long collectionId, Long productId);

    boolean existsByCollectionIdAndProductId(Long collectionId, Long productId);

    boolean existsByCollectionId(Long collectionId);

    void deleteByCollectionId(Long collectionId);

    /** Next append position; -1 default so the first item lands at display_order 0. */
    @Query("SELECT COALESCE(MAX(i.displayOrder), -1) FROM ProductCollectionItem i WHERE i.collectionId = :collectionId")
    int findMaxDisplayOrder(@Param("collectionId") Long collectionId);
}
