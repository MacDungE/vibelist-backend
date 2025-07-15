package org.example.vibelist.domain.post.like.repository;

import org.example.vibelist.domain.post.like.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);
    long countByCommentId(Long commentId);
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
}
