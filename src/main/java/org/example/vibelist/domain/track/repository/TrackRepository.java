package org.example.vibelist.domain.track.repository;

import org.example.vibelist.domain.track.entity.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {

    @Query("SELECT t FROM Track t JOIN FETCH t.audioFeature")
    List<Track> findAllWithAudioFeature();

    @Query("""
    SELECT t FROM Track t
    JOIN FETCH t.audioFeature
    WHERE t.id > :startTrackId
    ORDER BY t.id ASC
    """)
    List<Track> findByTrackIdAfter(@Param("startTrackId") Long startTrackId, Pageable pageable);
}
