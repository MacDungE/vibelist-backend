package org.example.vibelist.domain.audiofeature.repository;

import org.example.vibelist.domain.audiofeature.entity.AudioFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioFeatureRepository extends JpaRepository<AudioFeature, Long> {
}
