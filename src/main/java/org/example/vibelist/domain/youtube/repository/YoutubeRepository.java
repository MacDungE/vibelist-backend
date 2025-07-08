package org.example.vibelist.domain.youtube.repository;

import org.example.vibelist.domain.youtube.entity.Youtube;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YoutubeRepository extends JpaRepository<Youtube, Long> {
}
