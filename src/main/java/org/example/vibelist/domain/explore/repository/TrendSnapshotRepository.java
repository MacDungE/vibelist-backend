package org.example.vibelist.domain.explore.repository;

import org.example.vibelist.domain.explore.entity.TrendSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrendSnapshotRepository extends JpaRepository<TrendSnapshot, Long> {
    // 가장 최신의 COMPLETED 상태 스냅샷을 조회합니다.
    Optional<TrendSnapshot> findFirstByStatusOrderBySnapshotTimeDesc(TrendSnapshot.SnapshotStatus status);
}
