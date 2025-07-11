package org.example.vibelist.domain.batch.spotify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class SpotifyTrackMetaDto {
    private String artist;
    private String title;
    private String album;
    private int popularity;
    private int durationMs;
    private boolean explicit;
    private String imageUrl;
    private String spotifyId;
}
