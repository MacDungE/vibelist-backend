package org.example.vibelist.domain.playlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.vibelist.domain.playlist.es.document.AudioFeatureEsDocument;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "트랙 응답 DTO")
public class TrackRsDto {

    @Schema(description = "Spotify 트랙 ID", example = "6rqhFgbbKwnb9MLmUQDhG6", required = true)
    private String spotifyId;
    @Schema(description = "트랙 재생 시간(ms)", example = "210000", required = true)
    private Integer durationMs;

    private String trackId;
    private String title;
    private String artist;
    private String album;
    private int popularity;
    private boolean explicit;
    private String imageUrl;


    public static TrackRsDto from(AudioFeatureEsDocument doc) {
        return TrackRsDto.builder()
                .spotifyId(doc.getSpotifyId())
                .durationMs(doc.getDurationMs())
                .trackId(doc.getTrackMetrics().getTrackId())
                .title(doc.getTrackMetrics().getTitle())
                .artist(doc.getTrackMetrics().getArtist())
                .album(doc.getTrackMetrics().getAlbum())
                .popularity(doc.getTrackMetrics().getPopularity())
                .explicit(doc.getTrackMetrics().isExplicit())
                .imageUrl(doc.getTrackMetrics().getImageUrl())
                .build();
    }

}

