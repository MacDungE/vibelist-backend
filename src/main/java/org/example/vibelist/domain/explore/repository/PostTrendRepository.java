package org.example.vibelist.domain.explore.repository;

import org.example.vibelist.domain.explore.entity.PostTrend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostTrendRepository extends JpaRepository<PostTrend, Long> {
    // 특정 스냅샷 ID에 해당하는 트렌드 게시글을 조회합니다.
    // 💡 ManyToOne 관계로 변경됨에 따라 findBySnapshot_Id로 변경
    List<PostTrend> findBySnapshot_IdOrderByScoreDesc(Long snapshotId);

    // 특정 스냅샷 ID에 해당하는 모든 트렌드 게시글을 삭제합니다.
    // 💡 ManyToOne 관계로 변경됨에 따라 deleteBySnapshot_Id로 변경
    void deleteBySnapshot_Id(Long snapshotId);
}
