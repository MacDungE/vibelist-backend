package org.example.vibelist.domain.post.comment.repository;

import org.example.vibelist.domain.post.comment.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.user u WHERE c.post.id = :postId AND c.parent IS NULL")
    List<Comment> findParentByPostId(@Param("postId") Long postId, Sort sort);

    @Query("SELECT c FROM Comment c JOIN FETCH c.user u WHERE c.parent.id IN :parentIds")
    List<Comment> findChildByParentIds(@Param("parentIds") List<Long> parentIds);
}
