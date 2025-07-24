package org.example.vibelist.domain.post.like.repository;

import org.example.vibelist.domain.post.entity.Post;
import org.example.vibelist.domain.post.like.entity.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
    void deleteByPostIdAndUserId(Long postId, Long userId);

    List<PostLike> findByUserId(Long userId);

    @Query("SELECT pl.post FROM PostLike pl WHERE pl.user.id = :userId")
    List<Post> findLikedPostsByUserId(@Param("userId") Long userId);

    // 사용자가 좋아요한 모든 게시글 (페이지네이션)
    @Query("SELECT pl.post FROM PostLike pl " +
            "WHERE pl.user.id = :userId " +
            "AND pl.post.deletedAt IS NULL " +
            "ORDER BY pl.createdAt DESC")
    Page<Post> findLikedPostsByUserIdPageable(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT pl.post FROM PostLike pl " +
            "WHERE pl.user.username = :username " +
            "AND pl.post.deletedAt IS NULL " +
            "ORDER BY pl.createdAt DESC")
    Page<Post> findLikedPostsByUsernamePageable(@Param("username") String username, Pageable pageable);

    // 사용자가 좋아요한 공개 게시글만 (페이지네이션)
    @Query("SELECT pl.post FROM PostLike pl " +
            "WHERE pl.user.id = :userId " +
            "AND pl.post.isPublic = true " +
            "AND pl.post.deletedAt IS NULL " +
            "ORDER BY pl.createdAt DESC")
    Page<Post> findPublicLikedPostsByUserIdPageable(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT pl.post FROM PostLike pl " +
            "WHERE pl.user.username = :username " +
            "AND pl.post.isPublic = true " +
            "AND pl.post.deletedAt IS NULL " +
            "ORDER BY pl.createdAt DESC")
    Page<Post> findPublicLikedPostsByUsernamePageable(@Param("username") String username, Pageable pageable);

}