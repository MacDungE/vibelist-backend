package org.example.vibelist.domain.post.repository;

import org.example.vibelist.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
