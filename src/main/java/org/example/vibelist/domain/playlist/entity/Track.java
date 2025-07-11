package org.example.vibelist.domain.playlist.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.vibelist.domain.batch.audiofeature.entity.AudioFeature;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "track_id")
    private Long id;

    private String title;
    private String artist;
    private String album;
    private int popularity;
    private int durationMs;
    private boolean explicit;
    private String imageUrl;
    private String spotifyId;

    @OneToOne(fetch = FetchType.LAZY)   //지연로딩 적용 -> Auth 앤티티 조회할때 user 객체는 불러오지 않음
    @JoinColumn(name = "audio_feature_id") //auth.getUser()에 실제로 접근할 때 User 쿼리 발생!
    private AudioFeature audioFeature;


}
