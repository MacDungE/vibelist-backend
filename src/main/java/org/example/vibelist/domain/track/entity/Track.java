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
    private String durationMs;




    @OneToOne(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private AudioFeature audioFeature;

    @OneToOne(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private Youtube youtube;

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaylistTrack> playlistTracks = new ArrayList<>();

}
