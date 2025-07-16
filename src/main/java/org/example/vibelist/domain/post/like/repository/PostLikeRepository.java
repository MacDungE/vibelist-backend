package org.example.vibelist.domain.post.like.repository;

import org.example.vibelist.domain.post.entity.Post;
import org.example.vibelist.domain.post.like.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
    void deleteByPostIdAndUserId(Long postId, Long userId);

    List<Post> findPostsByUserId(Long userId);
}