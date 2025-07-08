package org.example.vibelist.domain.audiofeature.repository;

import org.example.vibelist.domain.audiofeature.entity.AudioFeature;
import org.example.vibelist.domain.track.entity.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AudioFeatureRepository extends JpaRepository<AudioFeature, Long> {

    Page<AudioFeature> findByTrackIsNull(Pageable pageable);

    Optional<Track> findByTrackId(Long id);

}
