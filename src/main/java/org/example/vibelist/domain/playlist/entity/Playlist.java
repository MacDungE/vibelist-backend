package org.example.vibelist.domain.playlist.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.vibelist.domain.playlisttrack.PlaylistTrack;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "playlist_id")

    private Long id;
    

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaylistTrack> playlistTracks = new ArrayList<>();
}
