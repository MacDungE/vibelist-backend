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
                        .fields("content",
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
    public static Query buildActivePostsSince(LocalDateTime since) {
        return Query.of(q -> q
                .bool(b -> b
                        .filter(f -> f
                                .bool(fb -> fb
                                        .mustNot(mn -> mn
                                                .exists(e -> e.field("deletedAt"))
                                        )
                                )
                        )
                        .filter(f -> f
                                .bool(b2 -> b2
                                        .should(s -> s
                                                .range(r -> r
                                                        .field("createdAt")
                                                        .gte(JsonData.of(since.format(ISO_DATE_TIME_FORMATTER)))
                                                )
                                        )
                                        .should(s -> s
                                                .range(r -> r
                                                        .field("updatedAt")
                                                        .gte(JsonData.of(since.format(ISO_DATE_TIME_FORMATTER)))
                                                )
                                        )
                                        .minimumShouldMatch("1")
                                )
                        )
                )
        );
    }

    /**
     * 최근 24시간 이내의 활성 게시글 중 좋아요 및 조회수를 기반으로 스코어링하고 정렬하는 쿼리를 빌드합니다.
     * 이 쿼리는 트렌드 스케줄러에서 사용됩니다.
     * @param since 조회 시작 시간 (예: 24시간 전)
     * @return Elasticsearch Query 객체
     */
    public static Query buildScoredAndSortedActivePosts(LocalDateTime since) {
        // 기본 쿼리: 활성 게시글 (deletedAt == null) 이면서 최근 24시간 내 생성/업데이트된 게시글
        Query baseQuery = Query.of(q -> q
                .bool(b -> b
                        .filter(f -> f // 활성 문서 필터링 (deletedAt이 없거나 null)
                                .bool(fb -> fb
                                        .mustNot(mn -> mn
                                                .exists(e -> e.field("deletedAt"))
                                        )
                                )
                        )
                        .filter(f -> f // createdAt 또는 updatedAt이 since 이후인 문서 필터링
                                .bool(b2 -> b2
                                        .should(s -> s
                                                .range(r -> r
                                                        .field("createdAt")
                                                        .gte(JsonData.of(since.format(ISO_DATE_TIME_FORMATTER)))
                                                )
                                        )
                                        .should(s -> s
                                                .range(r -> r
                                                        .field("updatedAt")
                                                        .gte(JsonData.of(since.format(ISO_DATE_TIME_FORMATTER)))
                                                )
                                        )
                                        .minimumShouldMatch("1")
                                )
                        )
                )
        );

        // function_score 쿼리를 사용하여 좋아요와 조회수 기반으로 스코어링
        return Query.of(q -> q
                .functionScore(fs -> fs
                        .query(baseQuery) // 기본 쿼리를 적용
                        .functions(f -> f
                                .fieldValueFactor(fvf -> fvf // 좋아요 수 기반 스코어
                                        .field("likeCnt")
                                        .factor(2.0) // 좋아요 수에 가중치 2.0
                                        .missing(0.0) // 필드가 없으면 0으로 간주
                                )
                                .weight(1.0) // 이 함수의 가중치 (전체 스코어에 미치는 영향)
                        )
                        .functions(f -> f
                                .fieldValueFactor(fvf -> fvf // 조회수 기반 스코어
                                        .field("viewCnt")
                                        .factor(1.0) // 조회수에 가중치 1.0
                                        .missing(0.0)
                                )
                                .weight(1.0)
                        )
                        .scoreMode(FunctionScoreMode.Sum) // 모든 함수 스코어의 합을 최종 스코어로 사용
                )
        );
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