package org.example.vibelist.domain.track.repository;

import org.example.vibelist.domain.track.entity.Track;
import org.example.vibelist.domain.youtube.entity.Youtube;
import org.springframework.beans.PropertyValues;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {
    Page<Track> findByYoutubeIsNull(Pageable pageable);

    List<Track> findAllByAudioFeatureId(Long audioFeatureId);

    List<Track> findByAudioFeatureIdIn(List<Long> audioFeatureIds);

    List<Track> findAllBySpotifyIdIn(List<String> spotifyIds);
}
