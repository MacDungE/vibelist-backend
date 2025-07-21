package org.example.vibelist.domain.explore.es;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class PostESQueryBuilder {

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private PostESQueryBuilder() { } // util-class

    /* ---------- 1. 키워드 검색 ---------- */
    public static Query search(String keyword) {
        return Query.of(q -> q.bool(b -> b
                .must(m -> m.multiMatch(mm -> mm
                        .fields("content","tagsAnalyzed",
                                "playlist.tracks.title",
                                "playlist.tracks.artist",
                                "playlist.tracks.album")
                        .query(keyword)))
                .filter(f -> f.term(t -> t.field("isPublic").value(true)))
        ));
    }

    /* ---------- 2. 피드(공개글) ---------- */
    public static Query feed() {
        return Query.of(q -> q.term(t -> t.field("isPublic").value(true)));
    }

    /* updatedAt desc 정렬 */
    public static List<SortOptions> sortByUpdatedAtDesc() {
        return List.of(
                SortOptions.of(s -> s
                        .field(f -> f.field("updatedAt").order(SortOrder.Desc)))
        );
    }

    /**
     * 특정 시간 범위 내에 생성되거나 업데이트된 활성(삭제되지 않은) 게시글 문서를 조회하는 쿼리를 빌드합니다.
     * 이 메서드는 일반적인 조회에 사용됩니다.
     * @param since 조회 시작 시간
     * @return Elasticsearch Query 객체
     */
    /** deletedAt 이 null 이고, createdAt 또는 updatedAt 이 `since` 이후인 글만 검색  */
    public static Query buildActivePostsSince(LocalDateTime since) {
        String iso = since.format(ISO_DATE_TIME_FORMATTER);   // ES 는 ISO-8601 문자열을 받음

        return Query.of(q -> q.bool(b -> b
                // ① deletedAt 존재 여부
                .filter(f -> f.bool(fb -> fb.mustNot(mn -> mn.exists(e -> e.field("deletedAt")))))

                // ② createdAt ≥ since  OR  updatedAt ≥ since  (최소 1개 조건 만족)
                .filter(f -> f.bool(b2 -> b2
                        .should(s -> s.range(r -> r.untyped(u -> u
                                .field("createdAt")
                                .gte(JsonData.of(iso))
                        )))
                        .should(s -> s.range(r -> r.untyped(u -> u
                                .field("updatedAt")
                                .gte(JsonData.of(iso))
                        )))
                        .minimumShouldMatch("1")      // should 절 둘 중 하나 이상
                ))
        ));
    }

    /**
     * 최근 24시간 이내의 활성 게시글 중 좋아요 및 조회수를 기반으로 스코어링하고 정렬하는 쿼리를 빌드합니다.
     * 이 쿼리는 트렌드 스케줄러에서 사용됩니다.
     * @param since 조회 시작 시간 (예: 24시간 전)
     * @return Elasticsearch Query 객체
     */
    public static Query buildScoredAndSortedActivePosts(LocalDateTime since) {
        String iso = since.format(ISO_DATE_TIME_FORMATTER);

        /* ── 1. 활성 + 최근 글 필터 ───────────────────────────── */
        Query baseQuery = Query.of(q -> q.bool(b -> b

                // deletedAt 이 null 인 문서만
                .filter(f -> f.bool(fb -> fb.mustNot(mn -> mn.exists(e -> e.field("deletedAt")))))

                // createdAt ≥ since  OR  updatedAt ≥ since  (둘 중 하나 이상)
                .filter(f -> f.bool(b2 -> b2
                        .should(s -> s.range(r -> r.untyped(u -> u
                                .field("createdAt")
                                .gte(JsonData.of(iso))
                        )))
                        .should(s -> s.range(r -> r.untyped(u -> u
                                .field("updatedAt")
                                .gte(JsonData.of(iso))
                        )))
                        .minimumShouldMatch("1")
                ))
        ));

        /* ── 2. 좋아요·조회수 기반 function_score ───────────────── */
        return Query.of(q -> q.functionScore(fs -> fs
                .query(baseQuery)

                // 좋아요 수
                .functions(f -> f.fieldValueFactor(fvf -> fvf
                        .field("likeCnt")
                        .factor(2.0)
                        .missing(0.0)
                ).weight(1.0))

                // 조회수
                .functions(f -> f.fieldValueFactor(fvf -> fvf
                        .field("viewCnt")
                        .factor(1.0)
                        .missing(0.0)
                ).weight(1.0))

                .scoreMode(FunctionScoreMode.Sum)
        ));
    }

    /**
     * Elasticsearch에서 스코어링된 결과를 _score 필드를 기준으로 내림차순 정렬하는 SortOptions를 빌드합니다.
     * @return SortOptions 리스트
     */
    public static List<SortOptions> buildScoreSortOptions() {
        return Collections.singletonList(
                SortOptions.of(so -> so
                        .score(s -> s.order(SortOrder.Desc)) // _score 필드를 기준으로 내림차순 정렬
                )
        );
    }
}