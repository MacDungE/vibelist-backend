package org.example.vibelist.domain.post.repository;

import org.example.vibelist.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByIdAndDeletedAtIsNull(Long id);
    @Query("""
        select p
          from Post p
          join fetch p.playlist pl
         where p.id = :id
           and p.deletedAt is null
    """)
    Optional<Post> findDetailById(Long id);

    // PostRepository에 추가
    List<Post> findByUserIdAndDeletedAtIsNull(Long userId);
    List<Long> findPostIdsByUserIdAndDeletedAtIsNull(Long userId);
    // 또는 더 효율적인 벌크 업데이트 방식
    @Modifying
    @Query("UPDATE Post p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.user.id = :userId AND p.deletedAt IS NULL")
    int softDeleteByUserId(@Param("userId") Long userId);
    // PostRepository에 추가 필요
    Page<Post> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);
    Page<Post> findByUserIdAndIsPublicTrueAndDeletedAtIsNull(Long userId, Pageable pageable);


//    Page<Post> findByUsernameAndDeletedAtIsNull(String username, Pageable pageable);
//    Page<Post> findByUsernameAndIsPublicTrueAndDeletedAtIsNull(String username, Pageable pageable);

    // username으로 조회 (연관관계 사용)
    @Query("SELECT p FROM Post p WHERE p.user.username = :username AND p.deletedAt IS NULL")
    Page<Post> findByUsernameAndDeletedAtIsNull(@Param("username") String username, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.username = :username AND p.isPublic = true AND p.deletedAt IS NULL")
    Page<Post> findByUsernameAndIsPublicTrueAndDeletedAtIsNull(@Param("username") String username, Pageable pageable);

}
