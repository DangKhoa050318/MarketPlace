package com.training.marketplace.repository;

import com.training.marketplace.entity.ContentHelpfulVote;
import com.training.marketplace.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentHelpfulVoteRepository extends JpaRepository<ContentHelpfulVote, Long> {

    Optional<ContentHelpfulVote> findByUserIdAndTargetTypeAndTargetId(Long userId, ContentType targetType, Long targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, ContentType targetType, Long targetId);

    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, ContentType targetType, Long targetId);

    List<ContentHelpfulVote> findByUserIdAndTargetTypeAndTargetIdIn(Long userId, ContentType targetType, List<Long> targetIds);
}
