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
    private int durationMs;

    public static TrackRsDto from(Track track) {
        // 추천 결과로 클라이언트에게 반환되는 트랙 정보를 담는 DTO
        // title, artist, spotifyId 등을 포함
        return TrackRsDto.builder()
                .trackId(String.valueOf(track.getId()))
                .title(track.getTitle())
                .artist(track.getArtist())
                .album(track.getAlbum())
                .imageUrl(track.getImageUrl())
                .spotifyId(track.getSpotifyId())
                .durationMs(track.getDurationMs())
                .build();
    }
}
