package org.example.vibelist.domain.playlisttrack;

import jakarta.persistence.*;
import lombok.Data;
import org.example.vibelist.domain.playlist.entity.Playlist;
import org.example.vibelist.domain.track.entity.Track;

import java.time.LocalDateTime;

@Entity
@Data
public class PlaylistTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "playlist_id")
    private Playlist playlist;

    @ManyToOne
    @JoinColumn(name = "track_id")
    private Track track;

    // 추가 필드 예시
    private int trackOrder; // 재생 순서
    private LocalDateTime addedAt; // 플레이리스트에 추가한 시간
}
