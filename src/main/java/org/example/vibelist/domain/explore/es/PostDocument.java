package org.example.vibelist.domain.explore.es;

import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.example.vibelist.domain.post.entity.Post;
import org.example.vibelist.domain.post.tag.entity.Tag;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.*;

import java.time.*;
import java.time.temporal.ChronoUnit;
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

    // 정확 매칭 + 집계용
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    // 부분 검색·자동완성용(선택)
    @Field(name = "tags_analyzed",
            type = FieldType.Text,
            analyzer = "standard")
    private List<String> tagsAnalyzed;


    private Boolean isPublic;
    private Long likeCnt;
    private Long viewCnt;



    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime createdAt;
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime updatedAt;

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

        var tags = post.getTags().stream()
                .map(Tag::getName)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .toList();

        return PostDocument.builder()
                .id(post.getId())
                .userId(post.getUser().getId())
                .userName(post.getUser().getUsername())
                .userProfileName(post.getUser().getUserProfile().getName())
                .content(post.getContent())
                .tags(tags)
                .tagsAnalyzed(tags)
                .isPublic(post.getIsPublic())
                .likeCnt(post.getLikeCnt())
                .viewCnt(post.getViewCnt())
                .createdAt(toUTC(post.getCreatedAt()))
                .updatedAt(toUTC(post.getUpdatedAt()))
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

        /* 2) 태그 리스트 정제 (공백·중복 제거, 소문자 통일) */
        var tags = dto.tags() == null
                ? List.<String>of()
                : dto.tags().stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();

        return PostDocument.builder()
                .id(dto.id())
                .userId(dto.userId())
                .userName(dto.userName())
                .userProfileName(dto.userProfileName())
                .content(dto.content())
                .tags(tags)
                .tagsAnalyzed(tags)
                .isPublic(dto.isPublic())
                .likeCnt(dto.likeCnt())
                .viewCnt(dto.viewCnt())
                .createdAt(toUTC(dto.createdAt()))
                .updatedAt(toUTC(dto.updatedAt()))
                .playlist(playlistDoc)
                .build();
    }

    private static OffsetDateTime toUTC(LocalDateTime t) {
        return t.atOffset(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);   // 6자리 µs 로 맞춤
    }


}