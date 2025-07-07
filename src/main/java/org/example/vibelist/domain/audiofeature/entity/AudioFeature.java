package org.example.vibelist.domain.audiofeature.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.vibelist.domain.track.entity.Track;

@Entity
@Data
public class AudioFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "audio_feature_id")
    private Long id;

    private double danceability;
    private double energy;
    private int key;
    private double loudness;
    private int mood;
    private double speechiness;
    private double acousticness;
    private double instrumentalness;
    private double liveness;
    private double valence;
    private double tempo;
    private int durationMs;
    private int timeSignature;
    private String spotifyId;
    private String genres;

    @OneToOne(fetch = FetchType.LAZY)   //지연로딩 적용 -> Auth 앤티티 조회할때 user 객체는 불러오지 않음
    @JoinColumn(name = "track_id") //auth.getUser()에 실제로 접근할 때 User 쿼리 발생!
    private Track track; // (선택적) 양방향 설정
}
