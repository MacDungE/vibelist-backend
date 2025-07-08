package org.example.vibelist.domain.track.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.vibelist.domain.audiofeature.entity.AudioFeature;
import org.example.vibelist.domain.playlisttrack.PlaylistTrack;
import org.example.vibelist.domain.youtube.entity.Youtube;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
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


    @OneToOne(fetch = FetchType.LAZY)   //지연로딩 적용 -> Auth 앤티티 조회할때 user 객체는 불러오지 않음
    @JoinColumn(name = "audio_feature_id") //auth.getUser()에 실제로 접근할 때 User 쿼리 발생!
    private AudioFeature audioFeature;

    @OneToOne(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private Youtube youtube;

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaylistTrack> playlistTracks = new ArrayList<>();

}
