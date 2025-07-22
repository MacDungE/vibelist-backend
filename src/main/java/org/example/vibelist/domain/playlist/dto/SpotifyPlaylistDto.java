package org.example.vibelist.domain.playlist.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Spotify 플레이리스트 응답 DTO")
public class SpotifyPlaylistDto {
    @Schema(description = "Spotify 플레이리스트 ID", example = "37i9dQZF1DXcBWIGoYBM5M", required = true)
    String spotifyId; //playlist에 대한 id
    // (필요시 추가 필드에 @Schema 적용)
}