package org.example.vibelist.domain.explore.es;

import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.example.vibelist.domain.post.entity.Post;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostDocument {

    @Id
    private Long id;

    private Long userId;
    private String userName;
    private String userProfileName;

    @Field(type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "standard")
    private String content;

    private Boolean isPublic;
    private Long likeCnt;
    private Long viewCnt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PlaylistDoc playlist;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlaylistDoc {
        private Long id;
        private String spotifyUrl;
        private Integer totalTracks;
        private Integer totalLengthSec;

        @Field(type = FieldType.Nested)
        private List<TrackDoc> tracks;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TrackDoc {
        private String spotifyId;
        private Integer durationMs;
        private String trackId;
        private String title;
        private String artist;
        private String album;
        private Integer popularity;
        private Boolean explicit;
        private String imageUrl;
    }

    public static PostDocument from(Post post) {
        var pl = post.getPlaylist();
        var playlistDoc = PlaylistDoc.builder()
                .id(pl.getId())
                .spotifyUrl(pl.getSpotifyUrl())
                .totalTracks(pl.getTotalTracks())
                .totalLengthSec(pl.getTotalLengthSec())
                .tracks(pl.getTracks().stream()
                        .map(t -> TrackDoc.builder()
                                .spotifyId(t.getSpotifyId())
                                .durationMs(t.getDurationMs())
                                .trackId(t.getTrackId())
                                .title(t.getTitle())
                                .artist(t.getArtist())
                                .album(t.getAlbum())
                                .popularity(t.getPopularity())
                                .explicit(t.isExplicit())
                                .imageUrl(t.getImageUrl())
                                .build())
                        .toList())
                .build();

        return PostDocument.builder()
                .id(post.getId())
                .userId(post.getUser().getId())
                .userName(post.getUser().getUsername())
                .userProfileName(post.getUser().getUserProfile().getName())
                .content(post.getContent())
                .isPublic(post.getIsPublic())
                .likeCnt(post.getLikeCnt())
                .viewCnt(post.getViewCnt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .playlist(playlistDoc)
                .build();
    }

    /**
     * PostDetailResponse DTO로부터 PostDocument를 생성합니다.
     * @param dto PostDetailResponse DTO
     * @return PostDocument 객체
     */
    public static PostDocument fromDto(PostDetailResponse dto) {
        var plDto = dto.playlist();
        var playlistDoc = PlaylistDoc.builder()
                .id(plDto.id())
                .spotifyUrl(plDto.spotifyUrl())
                .totalTracks(plDto.totalTracks())
                .totalLengthSec(plDto.totalLengthSec())
                .tracks(plDto.tracks().stream()
                        .map(t -> TrackDoc.builder()
                                .spotifyId(t.getSpotifyId())
                                .durationMs(t.getDurationMs())
                                .trackId(t.getTrackId())
                                .title(t.getTitle())
                                .artist(t.getArtist())
                                .album(t.getAlbum())
                                .popularity(t.getPopularity())
                                .explicit(t.isExplicit())
                                .imageUrl(t.getImageUrl())
                                .build())
                        .toList())
                .build();

        return PostDocument.builder()
                .id(dto.id())
                .userId(dto.userId())
                .userName(dto.userName())
                .userProfileName(dto.userProfileName())
                .content(dto.content())
                .isPublic(dto.isPublic())
                .likeCnt(dto.likeCnt())
                .viewCnt(dto.viewCnt())
                .createdAt(dto.createdAt())
                .updatedAt(dto.updatedAt())
                .playlist(playlistDoc)
                .build();
    }
}