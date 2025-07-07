package org.example.vibelist.domain.youtube.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.vibelist.domain.track.entity.Track;

@Entity
@Getter
public class Youtube {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "youtube_id")
    private Long id;

    private String url;
    private int durationSeconds;

    @OneToOne(fetch = FetchType.LAZY)   //지연로딩 적용 -> Auth 앤티티 조회할때 user 객체는 불러오지 않음
    @JoinColumn(name = "track_id") //auth.getUser()에 실제로 접근할 때 User 쿼리 발생!
    private Track track; // (선택적) 양방향 설정
}
