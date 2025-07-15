package org.example.vibelist.domain.post.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.post.support.TrackListConverter;
import org.example.vibelist.global.jpa.entity.BaseEntity;
import org.example.vibelist.global.jpa.entity.BaseTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "playlists")
//@EntityListeners(AuditingEntityListener.class)
public class Playlist extends BaseTime {

    /*-------------------- 기본 속성 --------------------*/
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    /** 원본 Spotify 플레이리스트 URL */
    @Column(name = "spotify_url", nullable = false, length = 255)
    private String spotifyUrl;

    /** 곡 수 & 총 길이(초) */
    @Column(name = "total_tracks", nullable = false)
    private Integer totalTracks;

    @Column(name = "total_length_sec", nullable = false)
    private Integer totalLengthSec;

    /*-------------------- 트랙 리스트(JSONB) --------------------*/

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tracks_json", columnDefinition = "jsonb", nullable = false)
    private List<TrackRsDto> tracks;

//    /*-------------------- 공통 메타 --------------------*/
//
//    @CreatedDate
//    @Column(name = "created_at", updatable = false)
//    private LocalDateTime createdAt;
//
//    @LastModifiedDate
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;

}