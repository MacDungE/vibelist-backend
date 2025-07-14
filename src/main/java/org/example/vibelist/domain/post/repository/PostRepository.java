package org.example.vibelist.domain.post.repository;

import org.example.vibelist.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

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

}
