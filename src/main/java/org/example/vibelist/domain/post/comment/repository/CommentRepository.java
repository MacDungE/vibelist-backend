package org.example.vibelist.domain.post.comment.repository;

import org.example.vibelist.domain.post.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.post.id = :postId")
    List<Comment> findAllByPostIdWithUser(@Param("postId")Long postId);

    @Query("""
    SELECT c FROM Comment c
    JOIN FETCH c.user
    LEFT JOIN FETCH c.children child
    LEFT JOIN FETCH child.user
    WHERE c.post.id = :postId
    """)
    List<Comment> findAllWithUserAndChildren(@Param("postId") Long postId);
}
