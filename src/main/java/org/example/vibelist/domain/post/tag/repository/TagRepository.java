package org.example.vibelist.domain.post.tag.repository;

import org.example.vibelist.domain.post.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    @Query("""
    SELECT t FROM tags t
    WHERE t.name >= :base AND t.name < :upper
    ORDER BY t.name
    LIMIT :limit
    """)
    List<Tag> findTopNByInitialRange(char base, char upper, int limit);

    @Query("""
    SELECT t FROM tags t
    WHERE lower(t.name) LIKE concat(lower(:prefix), '%')
    ORDER BY t.name
    LIMIT :limit
    """)
    List<Tag> findTopNByNamePrefix(String prefix, int limit);

    Optional<Tag> findByName(String name);
}
