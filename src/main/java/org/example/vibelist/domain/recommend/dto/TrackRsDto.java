package org.example.vibelist.domain.recommend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.vibelist.domain.track.entity.Track;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackRsDto {
    private String trackId;
    private String title;
    private String artist;
    private String album;
    private String imageUrl;
    private String spotifyId;

    public static TrackRsDto from(Track track) {
        return TrackRsDto.builder()
                .trackId(String.valueOf(track.getId()))
                .title(track.getTitle())
                .artist(track.getArtist())
                .album(track.getAlbum())
                .imageUrl(track.getImageUrl())
                .spotifyId(track.getSpotifyId())
                .build();
    }
}
