package com.training.marketplace.repository;

import com.training.marketplace.entity.ContentHelpfulVote;
import com.training.marketplace.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentHelpfulVoteRepository extends JpaRepository<ContentHelpfulVote, Long> {

    @Query("SELECT v FROM ContentHelpfulVote v WHERE v.user.id = :userId AND v.targetType = :targetType AND v.targetId = :targetId")
    Optional<ContentHelpfulVote> findByUserIdAndTargetTypeAndTargetId(@Param("userId") Long userId, @Param("targetType") ContentType targetType, @Param("targetId") Long targetId);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM ContentHelpfulVote v WHERE v.user.id = :userId AND v.targetType = :targetType AND v.targetId = :targetId")
    boolean existsByUserIdAndTargetTypeAndTargetId(@Param("userId") Long userId, @Param("targetType") ContentType targetType, @Param("targetId") Long targetId);

    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, ContentType targetType, Long targetId);

    @Query("SELECT v FROM ContentHelpfulVote v WHERE v.user.id = :userId AND v.targetType = :targetType AND v.targetId IN :targetIds")
    List<ContentHelpfulVote> findByUserIdAndTargetTypeAndTargetIdIn(@Param("userId") Long userId, @Param("targetType") ContentType targetType, @Param("targetIds") List<Long> targetIds);
}
