package org.example.vibelist.domain.explore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.vibelist.global.jpa.entity.BaseTime;

import java.time.LocalDateTime;

@Entity
@Table(name = "trend_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TrendSnapshot extends BaseTime {

    public enum SnapshotStatus {
        IN_PROGRESS, // 스냅샷 생성 중
        COMPLETED,   // 스냅샷 생성 완료
        FAILED       // 스냅샷 생성 실패
    }

    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime; // 스냅샷이 시작된 시간

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SnapshotStatus status; // 스냅샷 상태

    public void complete() {
        this.status = SnapshotStatus.COMPLETED;
    }

    public void fail() {
        this.status = SnapshotStatus.FAILED;
    }
}
