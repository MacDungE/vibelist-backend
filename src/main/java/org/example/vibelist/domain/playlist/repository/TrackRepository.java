package org.example.vibelist.domain.playlist.repository;

import org.example.vibelist.domain.playlist.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {

    List<Track> findAllByAudioFeatureId(Long audioFeatureId);

    List<Track> findByAudioFeatureIdIn(List<Long> audioFeatureIds);

    List<Track> findAllBySpotifyIdIn(List<String> spotifyIds);
}
