package org.example.vibelist.domain.explore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.vibelist.global.jpa.entity.BaseTime;


import java.time.LocalDateTime;

@Entity
@Table(name = "post_trends")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostTrend extends BaseTime {

    public enum TrendStatus {
        UP,     // 순위 상승
        DOWN,   // 순위 하강
        NEW,    // 새로 진입
        SAME,   // 순위 유지
        OUT     // 순위권 이탈 (이 필드에는 저장되지 않지만, 계산에 사용될 수 있음)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 💡 ManyToOne 관계로 변경
    @JoinColumn(name = "snapshot_id", nullable = false) // 💡 조인 컬럼 지정
    private TrendSnapshot snapshot; // 💡 TrendSnapshot 엔티티 참조

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "score", nullable = false)
    private Double score; // 스냅샷 시점의 최종 스코어

    @Column(name = "rank", nullable = false)
    private Integer rank; // 💡 현재 스냅샷에서의 순위

    @Column(name = "previous_rank")
    private Integer previousRank; // 💡 이전 스냅샷에서의 순위 (새로 진입 시 null)

    @Enumerated(EnumType.STRING)
    @Column(name = "trend_status", nullable = false)
    private TrendStatus trendStatus; // 💡 이전 스냅샷 대비 트렌드 상태 (상승, 하강, 신규, 유지)

    @Column(name = "rank_change", nullable = false)
    private Integer rankChange; // 💡 이전 스냅샷 대비 순위 변화량 (+2, -1 등)

    @Column(name = "post_content", columnDefinition = "text")
    private String postContent; // 게시글 내용 요약

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "user_profile_name")
    private String userProfileName;

    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime; // 스냅샷이 생성된 시간 (TrendSnapshot의 snapshotTime과 동일)

    // likeCnt, viewCnt, playlistSpotifyUrl, playlistImageUrl 필드 제거됨
}
