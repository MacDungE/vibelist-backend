package org.example.vibelist.domain.batch.spotify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Spotify 트랙 메타 응답 DTO")
@Builder
@Getter
@Setter
@AllArgsConstructor
public class SpotifyTrackMetaDto {
    @Schema(description = "트랙 제목", example = "Dynamite", required = true)
    private String title;
    @Schema(description = "아티스트명", example = "BTS", required = true)
    private String artist;
    private String album;
    private int popularity;
    private int durationMs;
    private boolean explicit;
    private String imageUrl;
    private String spotifyId;
}
