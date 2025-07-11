package org.example.vibelist.domain.playlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.vibelist.domain.playlist.es.document.AudioFeatureEsDocument;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackRsDto {

    private String spotifyId;
    private int durationMs;

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
                .trackId(String.valueOf(doc.getTrackMetrics().getId()))
                .title(doc.getTrackMetrics().getTitle())
                .artist(doc.getTrackMetrics().getArtist())
                .album(doc.getTrackMetrics().getAlbum())
                .popularity(doc.getTrackMetrics().getPopularity())
                .explicit(doc.getTrackMetrics().isExplicit())
                .imageUrl(doc.getTrackMetrics().getImageUrl())
                .build();
    }

}

