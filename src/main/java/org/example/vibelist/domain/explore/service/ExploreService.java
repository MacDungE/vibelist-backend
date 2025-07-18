package org.example.vibelist.domain.explore.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.explore.es.PostESQueryBuilder;
import org.example.vibelist.domain.post.dto.PlaylistDetailResponse;
import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.example.vibelist.domain.explore.es.PostDocument;

import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExploreService {

    private final ElasticsearchOperations operations;

    /* ---------- 검색 ---------- */
    public Page<PostDetailResponse> search(String keyword, Pageable pageable) {
        Query q = PostESQueryBuilder.search(keyword);
        NativeQuery nq = NativeQuery.builder()
                .withQuery(q)
                .withPageable(pageable)
                .build();
        return execute(nq, pageable);
    }

    /* ---------- 피드(최신순) ---------- */
    public Page<PostDetailResponse> feed(Pageable pageable) {
        Query q = PostESQueryBuilder.feed();
        NativeQuery nq = NativeQuery.builder()
                .withQuery(q)
                .withSort(Sort.by(Sort.Direction.DESC, "updatedAt"))
                .withPageable(pageable)
                .build();
        return execute(nq, pageable);
    }


    /* ---------- 공통 실행 + DTO 변환 ---------- */
    private Page<PostDetailResponse> execute(NativeQuery nq, Pageable pageable) {
        SearchHits<PostDocument> hits = operations.search(nq, PostDocument.class);
        List<PostDetailResponse> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toDto)
                .toList();
        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }


    /**
     * PostDetailResponse DTO를 받아서 PostDocument로 변환 후 Elasticsearch에 저장합니다.
     * 이 메서드는 PostService에서 호출될 예정입니다.
     * @param postDetailResponse 저장할 PostDetailResponse DTO
     */
    public void saveToES(PostDetailResponse postDetailResponse) {
        // PostDetailResponse를 PostDocument로 변환
        PostDocument postDocument = PostDocument.fromDto(postDetailResponse);
        // 변환된 PostDocument를 Elasticsearch에 저장
        operations.save(postDocument);
    }

    /**
     * Elasticsearch에서 특정 ID를 가진 PostDocument를 물리적으로 삭제합니다.
     * 이 메서드는 PostService에서 게시글 논리적 삭제 시 호출됩니다.
     * @param postId 삭제할 게시글의 ID
     */
    public void deleteFromES(Long postId) {
        // Elasticsearch에서 해당 ID의 문서를 물리적으로 삭제합니다.
        operations.delete(String.valueOf(postId), PostDocument.class);
    }


    private PostDetailResponse toDto(PostDocument doc) {
        var pl = doc.getPlaylist();
        var playlistDto = new PlaylistDetailResponse(
                pl.getId(),
                pl.getSpotifyUrl(),
                pl.getTotalTracks(),
                pl.getTotalLengthSec(),
                pl.getTracks().stream()
                        .map(t -> new TrackRsDto(
                                t.getSpotifyId(),
                                t.getDurationMs(),
                                t.getTrackId(),
                                t.getTitle(),
                                t.getArtist(),
                                t.getAlbum(),
                                t.getPopularity(),
                                t.getExplicit(),
                                t.getImageUrl()))
                        .toList()
        );

        return new PostDetailResponse(
                doc.getId(),
                doc.getUserId(),
                doc.getUserName(),
                doc.getUserProfileName(),
                doc.getContent(),
                doc.getTags(),
                doc.getIsPublic(),
                doc.getLikeCnt(),
                doc.getViewCnt(),
                fromUTC(doc.getCreatedAt()),
                fromUTC(doc.getUpdatedAt()),
                playlistDto
        );
    }
    private static LocalDateTime fromUTC(OffsetDateTime odt) {
        return odt
                .withOffsetSameInstant(ZoneOffset.UTC)   // 다른 오프셋이더라도 UTC 기준으로 환산
                .truncatedTo(ChronoUnit.MICROS)          // 6-µs 정밀도 통일
                .toLocalDateTime();                      // Offset 제거
    }

}